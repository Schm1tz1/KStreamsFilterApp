## Testdata with kcat
Example test data from sensors / home automation in JSON to test the filtering. Usage with kcat:
```bash
cat battery_1000.json | jq -c . | kcat -b localhost:9092 -t github.schm1tz1.first.input -P
cat thermal_comfort_1000.json | jq -c . | kcat -b localhost:9092 -t github.schm1tz1.second.input -P
```
