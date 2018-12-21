# Eclipse Vert.x / Istio Distributed Tracing  Booster

## Purpose
Showcase Istio’s Distributed Tracing via a (minimally) instrumented set of Eclipse Vert.x applications

## Prerequisites

* Openshift 3.10 cluster with Istio. For local development, download the latest release from [Maistra](https://github.com/Maistra/origin/releases) and run:

```bash
oc login -u system:admin
oc apply -f https://github.com/Maistra/openshift-ansible/raw/maistra-0.5.0/istio/cr-minimal.yaml -n istio-operator
oc get pods -n istio-system -w
```
Wait until the `openshift-ansible-istio-installer-job-xxxx` job has completed. It can take several minutes. The OpenShift console is available on https://127.0.0.1:8443.

```
oc adm policy add-cluster-role-to-user admin developer --as=system:admin
oc adm policy add-scc-to-user privileged -z default -n $(oc project -q)
oc login -u developer -p developer
```

## Build and deploy the application

### With Fabric8 Maven Plugin (FMP)
Execute the following command to build the project and deploy it to OpenShift:

```bash
mvn clean fabric8:deploy -Popenshift
```

Configuration for FMP may be found both in pom.xml and `src/main/fabric8` files/folders.

### With S2I 

```bash
find . | grep openshiftio | grep application | xargs -n 1 oc apply -f

oc new-app --template=vertx-istio-distributed-tracing-greeting-service -p SOURCE_REPOSITORY_URL=https://github.com/openshiftio-vertx-boosters/vertx-istio-distributed-tracing-booster -p SOURCE_REPOSITORY_REF=master -p SOURCE_REPOSITORY_DIR=vertx-istio-distributed-tracing-greeting-service
oc new-app --template=vertx-istio-distributed-tracing-cute-name-service -p SOURCE_REPOSITORY_URL=https://github.com/openshiftio-vertx-boosters/vertx-istio-distributed-tracing-booster -p SOURCE_REPOSITORY_REF=master -p SOURCE_REPOSITORY_DIR=vertx-istio-distributed-tracing-cute-name-service
```


## Use Cases

### Configure an ingress Route to access the application


```bash
oc create -f rules/gateway.yaml
```

### Access the application

Run the following command to determine the appropriate URL to access our demo. Make sure you access the url with the HTTP scheme. HTTPS is NOT enabled by default:

```bash
echo "http://$(oc get route istio-ingressgateway -o jsonpath='{.spec.host}{"\n"}' -n istio-system)"/greeting
```

Open this URL in your a web browser.

### Accessing the traces

Once you used the application a little bit, you should be able to see the traces.


```bash
echo "https://$(oc get route/tracing -n istio-system  -o 'jsonpath={.spec.host}')"
# Open the url in your browser, you may have to accept the certificate
```
