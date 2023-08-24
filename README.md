# Plugins Manager for Apache JMeter

[Official Website](https://jmeter-plugins.org/wiki/PluginsManager/)

![pmgr screenshot](https://jmeter-plugins.org/img/wiki/pmgr/pmgr_dialog.png)

## Installation options

A prerequisite is to install `cmdrunner`:
```
cd ${JMETER_HOME}/lib && wget https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.2.1/cmdrunner-2.2.1.jar
```

### Option 1 - manual, get the latest version

In your web browser, navigate to https://jmeter-plugins.org/get/. This will download the latest jar file of the JMeter Plugins Manager. Put it in the `${JMETER_HOME}/lib/ext` directory.

### Option 2 - automated, get the latest version

Same as option 1, but you can run it from a script:
```
cd ${JMETER_HOME}/lib/ext && wget https://jmeter-plugins.org/get/ --output-document=jmeter-plugins-manager.jar
```

### Option 3 - automated, get a specific version

```
JMETER_PLUGINS_MANAGER_VERSION="1.10"
cd ${JMETER_HOME}/lib/ext && wget https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/${JMETER_PLUGINS_MANAGER_VERSION}/jmeter-plugins-manager-${JMETER_PLUGINS_MANAGER_VERSION}.jar
```

### References
* https://jmeter-plugins.org/wiki/PluginsManagerAutomated/
* https://medium.com/@praveenkrjha93/create-a-docker-file-for-jmeter-along-with-the-plugins-2b8af0eabe4a
