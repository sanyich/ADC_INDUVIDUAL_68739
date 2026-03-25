# ADC-PEI 25/26 — First Web Application (Part 4)

Building on Part 3, this version introduces **Google Cloud Tasks** to offload long-running computations asynchronously, and adds a **protected secret page** accessible only via the web interface.

---

## What changed from Part 3

- A new **`GET /rest/utils/compute`** endpoint was added that triggers an async task via **Google Cloud Tasks** instead of blocking the request thread
- A **`webapp/secret/index.html`** page was added, intended to be accessible only to authenticated users

---

## What this app does

The app serves a simple web page with the following available services:

- **`POST /rest/login/`** —  authenticates a user and returns a JSON auth token
- **`GET /rest/login/{username}`** — checks whether a username is already taken
- **`GET /rest/utils/hello`** — intentionally throws an exception and redirects to `/error/500.html`
- **`GET /rest/utils/time`** — returns the current server time in JSON
- **`GET /rest/utils/compute`** — enqueues an async computation task via Cloud Tasks

---

## Cloud Tasks

The `/rest/utils/compute` endpoint demonstrates how to offload a long-running task (e.g. a 10-minute computation) without blocking the HTTP request. Instead of running it inline, it enqueues a task to **Google Cloud Tasks**, which then triggers a `POST /rest/utils/compute` on the App Engine backend asynchronously.

> ⚠️ Cloud Tasks only works when deployed to Google App Engine. It will not work when running locally with `mvn appengine:run`.

---

## Prerequisites

Before you begin, make sure you have the following installed:

- **Google Cloud Account**
  > ⚠️ See the instructions to redeem your cupon in shared materials: **`ADC_Project_Google_Cloud_Account_Creation.pdf`**
- [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- [Python 3.10](https://www.python.org/downloads/release/python-3100/) (for the Google Cloud SDK download)
- [Apache Maven](https://maven.apache.org/install.html)
- [Git](https://git-scm.com/)
- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) (for cloud deployment)
- [Eclipse IDE](https://www.eclipse.org/downloads/) with the Maven plugin

---

## Getting Started

### 1. Fork and clone the repository

Fork the project on GitHub, then clone your fork locally:

```bash
git clone git@github.com:ADC-Projeto/adc-pei-2526-part4.git
cd adc-pei-2526-part4
```

### 2. Import into Eclipse

1. Open Eclipse and go to **File → Import → Maven → Existing Maven Projects**
2. Navigate to the folder where you cloned the project
3. Select it and click **Finish**
4. Eclipse will resolve dependencies automatically — check for any errors in the **Problems** tab

---

## Building the project

From the project root, run:

```bash
mvn clean package
```

If the build succeeds, you'll find the compiled `.war` file at:

```
target/Firstwebapp-0.0.1.war
```

---

## Running locally

Start the local App Engine dev server with:

```bash
mvn appengine:run
```

Then open your browser and go to:

```
http://localhost:8080/
```

---

## Deploying to Google App Engine

### 1. Create a project on Google Cloud Console

Go to https://console.cloud.google.com/ and create a new project. Take note of the Project ID.

### 2. Enable Cloud Tasks

In the Google Cloud Console, enable the **Cloud Tasks API** for your project and make sure a queue named `Default` exists in your chosen region.

### 3. Update `ComputationResource.java`

Replace the placeholder project ID before deploying:

```java
String projectId = "your-project-id"; // ← replace with your GCP project ID
```

### 4. Authenticate with Google Cloud

```bash
gcloud auth login
gcloud config set project <your-proj-id>
```

### 5. Deploy

```bash
mvn appengine:deploy -Dapp.deploy.projectId=<your-proj-id> -Dapp.deploy.version=<version-number>
```

After a successful deploy, your app will be live at:

```
https://<your-project-id>.appspot.com/
```

### Testing with Postman

1. Open [Postman](https://www.postman.com/downloads/) and create a new request
2. Set the method to GET and the URL to `https://<your-project-id>.appspot.com/rest/utils/compute`
3. Click Send

Returns `200 OK` immediately, while the actual computation is processed asynchronously in the background by Cloud Tasks.

---

## Project Structure

```
src/
└── main/
    ├── java/
    │   └── pt/unl/fct/di/adc/firstwebapp/
    │       ├── filters/
    │       │   └── AdditionalResponseHeadersFilter.java  ← CORS filter
    │       ├── resources/
    │       │   ├── ComputationResource.java              ← Utility endpoints + Cloud Tasks trigger
    │       │   └── LoginResource.java                    ← Login endpoint
    │       └── util/
    │           ├── AuthToken.java                        ← Token model
    │           └── LoginData.java                        ← Login request model
    └── webapp/
        ├── index.html                                    ← Front page
        ├── secret/
        │   └── index.html                                ← Protected page
        ├── error/
        │   ├── 404.html                                  ← Custom 404 page
        │   └── 500.html                                  ← Custom 500 page
        ├── img/
        │   ├── cat.png
        │   └── jedi.gif
        └── WEB-INF/
            ├── web.xml                                   ← Servlet config
            └── appengine-web.xml                         ← App Engine config
```

---

## License

See [LICENSE](LICENSE) for details.

---

*FCT NOVA — ADC-PEI 2025/2026*
