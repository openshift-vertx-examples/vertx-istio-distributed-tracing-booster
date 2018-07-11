# Eclipse Vert.x / Istio Distributed Tracing  Booster

## Purpose
Showcase Istio’s Distributed Tracing via a (minimally) instrumented set of Eclipse Vert.x applications

## Prerequisites
* Openshift 3.9 cluster
* Istio
* Create a new project/namespace on the cluster. This is where your application will be deployed.

```bash
oc login -u system:admin
oc adm policy add-cluster-role-to-user admin developer --as=system:admin
oc login -u developer -p developer
oc new-project <whatever valid project name you want> # not required
```

## Build and deploy the application

### With Fabric8 Maven Plugin (FMP)
Execute the following command to build the project and deploy it to OpenShift:

```bash
mvn clean fabric8:deploy -Popenshift
```

Configuration for FMP may be found both in pom.xml and `src/main/fabric8` files/folders.


## Use Cases

### Configure an ingress Route to access the application


```bash
oc create -f rules/redirection.yaml
```

### Access the application

Run the following command to determine the appropriate URL to access our demo. Make sure you access the url with the HTTP scheme. HTTPS is NOT enabled by default:

```bash
echo http://$(oc get route istio-ingressgateway -o jsonpath='{.spec.host}{"\n"}' -n istio-system)/greeting

```

The result of the above command is the `istio-system` `istio-ingress` URL, appended with the `RouteRule` path. Open this URL in your a web browser.

### Accessing the traces

Once you used the application a little bit, you should be able to see the traces.


```bash
echo https://$(oc get route/tracing -n istio-system  -o 'jsonpath={.spec.host}')
# You may have to accept the certificate
```
