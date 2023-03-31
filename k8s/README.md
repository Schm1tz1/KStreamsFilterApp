# Kubernetes Example (TODO)

## KStreams to CCloud Connection 

- Create Secrets with Configs:
```bash
kubectl create secret generic kstreams --from-file=kstreams.properties=./streams_ccloud.properties
```
- Deploy Application (./app/):
```bash
kubectl apply -f kstreams-app.yaml
```

## Additional CSM Service (./service/)
- export and re-import your local docker image, example (k3s cluster):
```bash
# export
docker save --output csm-encryption-aws-v1.0.5-SNAPSHOT.tar us-docker.pkg.dev/csid-281116/csid-docker-repo-us/csm-encryption-aws:1.0.5-SNAPSHOT

# copy to k3s master
rsync -v csm-encryption-aws-v1.0.5-SNAPSHOT.tar k8s-master:~/

# import into k3s
sudo k3s ctr images import csm-encryption-aws-v1.0.5-SNAPSHOT.tar

# check if it was imported correctly
sudo k3s ctr images ls | grep csm-encryption-aws 
```
- create services for all possible CSM ports from a template with **gomplate**:
```bash
gomplate -f csm-service.template >csm-service.yaml
```
- Create Secrets with Configs:
```bash
kubectl create secret generic csm-properties --from-file=csm.properties=./csm-aws-ple.properties
```
## KStreams with CSM sidecar (./sidecar/)
- Create Secrets with Configs:
```bash
kubectl create secret generic kstreams-csm-sidecar --from-file=kstreams.properties=./streams_csm_sidecar.properties
kubectl create secret generic csm-sidecar-properties --from-file=csm.properties=./csm-sidecar-aws-ple.properties
```
- Deploy:
```bash
kubectl apply -f kstreams-csm-sidecar.yaml
```
- Watch sidecar container logs:
```bash
kubectl logs kstreams-sidecar-app-XXXXX -c csm-sidecar -f
```
- Use kcat in container do look at topics:
```bash
# directly connected to CC
kcat -b pkc-e8mp5.eu-west-1.aws.confluent.cloud:9092 -X security.protocol=sasl_ssl -X sasl.mechanisms=PLAIN -X sasl.username={{CCloud-API-Key}}  -X sasl.password={{CCloud-API-Secret}} -t ha_filtered -C -o0

## via CSM
kcat -b localhost:30001 -X security.protocol=sasl_plaintext -X sasl.mechanisms=PLAIN -X sasl.username={{CCloud-API-Key}}  -X sasl.password={{CCloud-API-Secret}} -t ha_filtered -C -o0
```
