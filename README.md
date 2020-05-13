# Test cases for Payara issue 4307

This repository builds a Java Enterprise Application to be deployed into Payara Server.

Submodules:

| Module       | Purpose
|--------------|---------
| `common-jar` | A JAR file to be loaded from the AppServer's lib directory
| `app-jar`    | A JAR file to be loaded from the EAR's lib directory
| `war`        | A WAR file that uses `app-jar` and contains a simple servlet for ClassLoader debugging
| `ear`        | An EAR that deploys the war and app-jar
| `ear-docker` | Dockerfile for Payara with EAR deployment
| `war-docker` | Dockerfile for Payara with WAR deployment


## Scenario

We want to deploy an EAR containing multiple skinny WARs that share common classes from `app-jar` in lib/*.jar.
An additional `common-jar` is placed in the App Server's lib directory:

```
payaratest-ear.ear
|
+-- payaratest-war.war (with manifest Class-Path: lib/payaratest-app-jar.jar)
|
+-- payaratest-war-second.war (with manifest Class-Path: lib/payaratest-app-jar.jar)
|
+-- lib
    |
    +-- payaratest-app-jar.jar

payaratest-common-jar.jar
```

Thus, the ClassLoader hierarchy should look something like this:

- A Webapp ClassLoader for loading classes from each of the WARs (in this example we use two copies of our `war`)
- The Ear ClassLoader for loading classes from the `app-jar` in the EAR's lib directory
- The Common ClassLoader for loading classes from the `common-jar` in the App Server's lib directory
- The Module ClassLoader for loading classes from Payara's built-in modules

```
+--------------------+
|                    |
|  +-----+  +-----+  |
|  | WAR |  | WAR |  |
|  +-----+  +-----+  |
|      \      /      |
|    +---------+     |
|    | app-jar |     |
|    +---------+     |
|EAR      |          |
+---------+----------+
          |
   +------------+
   | common-jar |
   +------------+
          |
   +------------+
   | AS modules |
   +------------+
          |
   +------------+
   |     JVM    |
   +------------+
```


## Building

Build JARs, WAR and EAR:

```
mvn clean package
```

Build the Docker image:

```
# To use Payara 5.194:
./ear-docker/build.sh
# To use Payara 4.181:
./ear-docker/build.sh Payara4
```

Run the Docker image:

```
./ear-docker/run.sh
```

Home page of first WAR:

http://localhost:8082/payaratest/

Home page of second WAR:

http://localhost:8082/payaratest2/

The top of these pages show what each ClassLoader does when asked to load a given class or resource,
from the perspective of a class deployed inside the WAR (WebContextListenerClassLoader) vs. as an EAR JAR (LibContextListenerClassLoader).
An attempt will be made to load a class if the resource name ends with a `.class` like `com/github/rtwruck/common/ClassLoadedFromCommonLib.class`.

Some resources to try:

- Webapp ClassLoader / war: `com/github/rtwruck/web/ClassLoadedFromWar.class`
- EAR ClassLoader / app-jar: `com/github/rtwruck/app/ClassLoadedFromEarLib.class`
- Common ClassLoader / common-jar and NOT whitelisted: `com/github/rtwruck/common/ClassLoadedFromCommonLib.class`
- Common ClassLoader / common-jar and whitelisted  in glassfish-application.xml: `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class`
- Module ClassLoader and NOT whitelisted: `org/apache/taglibs/standard/Version.class`
- Module ClassLoader and whitelisted in glassfish-application.xml: `org/objectweb/asm/ClassWriter.class`

A resource that is present in ALL of these:

- Any ClassLoader: `org/objectweb/asm/ClassTooLargeException.class`

The bottom of the page prints each well-known ClassLoader:

- The current Thread's context ClassLoader
- The Webapp ClassLoader that loaded the Servlet
- The EAR ClassLoader (the one that successfully loaded `com.github.rtwruck.app.ClassLoadedFromEarLib`)
- The common ClassLoader (the one that successfully loaded `com.github.rtwruck.common.ClassLoadedFromCommonLib`)
- The module ClassLoader (the one that loaded the Webapp ClassLoader)


## Test cases with default class loading

- Disable Extreme Classloading Isolation by removing the `whitelist-package`s from [ear/src/main/application/META-INF/glassfish-application.xml]
- Build and run the Docker image

### 1. No whitelisting - Class accessibility

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fweb%2FClassLoadedFromWar.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fcommon%2FClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fapache%2Ftaglibs%2Fstandard%2FVersion.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassWriter.class

**Expected result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` is accessible and loaded from the Webapp ClassLoader
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is accessible and loaded from the EAR ClassLoader
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` is accessible and loaded from the common ClassLoader
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is accessible and loaded from the common ClassLoader
- `org/apache/taglibs/standard/Version.class` is accessible and loaded from the module ClassLoader
- `org/objectweb/asm/ClassWriter.class` is accessible and loaded from the module ClassLoader

**Actual result:**

- All OK

**Payara 4:**

- Same


### 2. No whitelisting - Resource visibility

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fweb%2FClassLoadedFromWar.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fcommon%2FClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fapache%2Ftaglibs%2Fstandard%2FVersion.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassWriter.class

**Expected result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` when looked up via `findResource` is found exactly once
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` when looked up via `findResource` is found exactly once
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` when looked up via `findResource` is found exactly once
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` when looked up via `findResource` is found exactly once
- `org/apache/taglibs/standard/Version.class` when looked up via `findResource` is found exactly once
- `org/objectweb/asm/ClassWriter.class` when looked up via `findResource` is found exactly once

**Actual result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` OK
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is found twice with the same URL by the Webapp ClassLoader and once when loaded from the other ones
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` is found twice with the same URL by any ClassLoader
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is found twice with the same URL by any ClassLoader
- `org/apache/taglibs/standard/Version.class` is found twice with the same URL by any ClassLoader
- `org/objectweb/asm/MethodTooLargeException.class` is found twice with the same URL by any ClassLoader

**Payara 4:**

- Same


### 3. No whitelisting - Delegation

- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassTooLargeException.class

**Expected result:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the module ClassLoader

**Actual result:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the EAR ClassLoader (`app-jar`)

**Payara 4:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the common ClassLoader (`common-jar`)


### 4. No whitelisting - EAR class sharing

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Compare with http://localhost:8082/payaratest2/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class

**Expected result:**

- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is referring to the same instance (ID) on both Webapps

**Actual result:**

- OK

**Payara 4:**

- Same


### 5. No whitelisting - Common class sharing

- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Compare with http://localhost:8082/payaratest2/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class

**Expected result:**

- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is referring to the same instance (ID) on both Webapps

**Actual result:**

- OK

**Payara 4:**

- Same


## Test cases with Extreme Classloading Isolation

- Enable Extreme Classloading Isolation by adding the `whitelist-package`s in [ear/src/main/application/META-INF/glassfish-application.xml]
- Build and run the Docker image

### 6. Whitelisting - Class accessibility

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fweb%2FClassLoadedFromWar.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fcommon%2FClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fapache%2Ftaglibs%2Fstandard%2FVersion.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassWriter.class

**Expected result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` is accessible and loaded from the Webapp ClassLoader
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is accessible and loaded from the EAR ClassLoader
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` is NOT accessible
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is accessible and loaded from the common ClassLoader
- `org/apache/taglibs/standard/Version.class` is NOT accessible
- `org/objectweb/asm/ClassWriter.class` is accessible and loaded from the module ClassLoader

**Actual result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` OK
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is accessible but loaded from the Webapp ClassLoader
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` OK
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` OK
- `org/apache/taglibs/standard/Version.class` OK
- `org/objectweb/asm/ClassWriter.class` OK

**Payara 4:**

- Same


### 7. Whitelisting - Resource visibility

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fweb%2FClassLoadedFromWar.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fcommon%2FClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Visit http://localhost:8082/payaratest/?q=org%2Fapache%2Ftaglibs%2Fstandard%2FVersion.class
- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassWriter.class

**Expected result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` when looked up via `findResource` is found exactly once
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` when looked up via `findResource` is found exactly once
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` is NOT found
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` when looked up via `findResource` is found exactly once
- `org/apache/taglibs/standard/Version.class` is NOT found
- `org/objectweb/asm/ClassWriter.class` when looked up via `findResource` is found exactly once

**Actual result:**

- `com/github/rtwruck/web/ClassLoadedFromWar.class` OK
- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is found twice with the same URL when loaded from the Webapp ClassLoader and once when loaded from the other ones
- `com/github/rtwruck/common/ClassLoadedFromCommonLib.class` is found twice with the same URL in any ClassLoader
- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is found twice with the same URL in any ClassLoader
- `org/apache/taglibs/standard/Version.class` is found twice with the same URL in any ClassLoader
- `org/objectweb/asm/ClassWriter.class` is found twice with the same URL in any ClassLoader

**Payara 4:**

- Same


### 8. Whitelisting - Delegation

- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FClassTooLargeException.class

**Expected result:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the module ClassLoader

**Actual result:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the EAR ClassLoader (`app-jar`)

**Payara 4:**

- `org/objectweb/asm/ClassTooLargeException.class` is loaded from the common ClassLoader (`common-jar`)


### 9. Whitelisting - EAR lib class sharing

- Visit http://localhost:8082/payaratest/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class
- Compare with http://localhost:8082/payaratest2/?q=com%2Fgithub%2Frtwruck%2Fapp%2FClassLoadedFromEarLib.class

**Expected result:**

- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` is referring to the same instance (ID) on both Webapps

**Actual result:**

- `com/github/rtwruck/app/ClassLoadedFromEarLib.class` has a different ID and ClassLoader in each Webapp

**Payara 4:**

- Same


### 10. Whitelisting - Common class sharing

- Visit http://localhost:8082/payaratest/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class
- Compare with http://localhost:8082/payaratest2/?q=org%2Fobjectweb%2Fasm%2FWhitelistedClassLoadedFromCommonLib.class

**Expected result:**

- `org/objectweb/asm/WhitelistedClassLoadedFromCommonLib.class` is referring to the same instance (ID) on both Webapps

**Actual result:**

- OK

**Payara 4:**

- Same
