#!/bin/sh -xe
jmeter -t $(dirname $0)/plugins-map.jmx -n
python check-descriptors.py jmeter.log ~/Sources/JMeter/jmeter-plugins/site/dat/repo/