import json
import os

import sys, logging

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    with open(sys.argv[1]) as fhd:
        lines = fhd.readlines()

    while lines:
        line = lines.pop(0)
        if "Plugin Components:" in line.strip():
            logging.debug("Found start: %s", line)
            break

    plugins = {}
    while lines:
        line = lines.pop(0)
        #logging.debug(line)
        if not line.strip():
            break

        plugins[line.strip()] = json.loads(lines.pop(0).strip()[len('"componentClasses":'):-1])

    #logging.info("%s", plugins)

    for fname in os.listdir(sys.argv[2]):
        fname = os.path.join(sys.argv[2], fname)
        logging.info(fname)
        with open(fname) as fhd:
            fpls = json.loads(fhd.read())

        for plugin in fpls:
            if plugin['id'] not in plugins or not plugin['markerClass']:
                logging.warning("Missing: %s", plugin['id'])
                continue

            if not 'componentClasses' in plugin:
                plugin['componentClasses'] = []

            if set(plugin['componentClasses']) != set(plugins[plugin['id']]):
                diff = json.dumps(list(set(plugins[plugin['id']]) - set(plugin['componentClasses'])))
                logging.debug("Diff %s %s: %s", plugin['id'], diff, json.dumps(plugins[plugin['id']]))
