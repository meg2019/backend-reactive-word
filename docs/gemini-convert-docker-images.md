# Building Docker Images for GKE (Converting arm64 to amd64)

## Do you need to convert your images?
**Yes, in most cases.** 
When you build a Docker image on a Mac with an Apple Silicon chip (M1/M2/M3/M4), Docker builds it for the `arm64` architecture by default. 

Google Kubernetes Engine (GKE) primarily uses standard Intel/AMD processors which run on the `amd64` (also known as `x86_64`) architecture. If you try to run an `arm64` image on an `amd64` GKE node, your pods will crash with an error similar to `standard_init_linux.go: exec user process caused "exec format error"`.

*(Note: GKE does support `arm64` nodes using Tau T2A machine types, but unless you explicitly created your GKE cluster with an `arm64` node pool, you are using `amd64`.)*

## How to build amd64 images on your Mac (M4)

You don't technically "convert" an already built image; instead, you **rebuild** the image from your `Dockerfile` and tell Docker to target the `amd64` platform.

Docker Desktop for Mac includes `buildx`, a CLI plugin that extends the `docker build` command with full support for building multi-platform images using QEMU emulation.

Here is the step-by-step guide:

### Step 1: Verify `buildx` is enabled
Docker Desktop usually has this enabled by default. You can check by running:
```bash
docker buildx version
```

### Step 2: Build the image for `amd64`
Navigate to the directory containing your `Dockerfile` and use the `--platform linux/amd64` flag explicitly.

**Standard Build Command:**
```bash
docker build --platform linux/amd64 -t your-username/your-image-name:tag .
```
*Example:*
```bash
docker build --platform linux/amd64 -t my-gcp-project/my-app:v1.0 .
```

### Step 3: (Optional but Recommended) Build Multi-Architecture Images
If you want an image that works on BOTH your Mac (arm64) and GKE (amd64) under the same tag, you can build a multi-arch image. 

First, create a new builder instance (you only need to do this once):
```bash
docker buildx create --use --name multi-arch-builder
```

Then, build and push for both platforms. *(Note: when building for multiple platforms simultaneously, Docker requires you to output the image to a registry directly using `--push` rather than loading it into your local Mac Docker daemon.)*
```bash
docker buildx build --platform linux/amd64,linux/arm64 -t your-registry/your-image-name:tag --push .
```

### Step 4: Verify the Image Architecture
If you used the command from Step 2, you can verify your image was built for the correct architecture before deploying to GKE:
```bash
docker inspect your-username/your-image-name:tag | grep Architecture
```
It should return:
```json
"Architecture": "amd64"
```

### Step 5: Push to Google Artifact Registry (GAR)
Before GKE can use your image, it needs to be pushed to a container registry accessible by your GKE cluster, like Google Artifact Registry.

1. **Tag your image for GAR:**
   ```bash
   docker tag your-username/your-image-name:tag REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/IMAGE:tag
   ```
2. **Configure Docker to authenticate with GCP:**
   ```bash
   gcloud auth configure-docker REGION-docker.pkg.dev
   ```
3. **Push the image:**
   ```bash
   docker push REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/IMAGE:tag
   ```

You are now ready to reference this image URL in your Kubernetes deployment YAML files and deploy it to GKE!

---

## Building an amd64 Image directly using Quarkus

If your application is built using [Quarkus](https://quarkus.io), you don't even need to run explicit `docker build` commands manually! Quarkus has built-in container-image extensions that handle multi-architecture and platform-specific builds right from your Java project.

Depending on which container-image extension you are using, add the following properties to your `src/main/resources/application.properties` file:

### Option 1: Using the Jib Extension (`quarkus-container-image-jib`)
Jib builds container images entirely natively in Java without needing a Docker daemon, making it exceptionally fast and bypassing the Mac's `arm64` limitation completely. To build an `amd64` image:
```properties
# Tell Jib to explicitly build for the amd64 architecture (GKE's default)
quarkus.jib.platforms=linux/amd64

# (Optional) To build a multi-arch image (works on both your Mac and GKE):
# quarkus.jib.platforms=linux/amd64,linux/arm64
```

### Option 2: Using the Docker Extension (`quarkus-container-image-docker`)
If you prefer the Docker extension (which uses Dockerfiles under the hood), it will leverage your local Docker Desktop's `buildx` capabilities automatically:
```properties
# Tell the Quarkus Docker extension to target amd64
quarkus.docker.buildx.platform=linux/amd64

# (Optional) To build a multi-arch image:
# quarkus.docker.buildx.platform=linux/amd64,linux/arm64
```
*Note: If you specify multiple platforms (multi-arch build), Docker `buildx` will not load the image into your local Docker daemon. You will also need to configure `quarkus.container-image.push=true` to push it directly to your remote registry (e.g., Google Artifact Registry).*

### Option 3: Building Native Executables (GraalVM) for GKE
If you are compiling a Quarkus Native executable to run in a container, you must instruct the builder to target `amd64` and ensure the compilation happens safely inside a container instead of directly on your Mac:
```properties
# Tell the native image builder to compile for amd64
quarkus.native.container-runtime-options=--platform=linux/amd64

# Ensure the native compilation happens inside a container environment matching the target arch
quarkus.native.container-build=true
```

### Triggering the Build
Once you've configured your `application.properties` (with your registry mapping like `quarkus.container-image.group`), triggering the GKE-ready image build is as simple as running your standard Quarkus command:
```bash
./mvnw clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true
```

---

## Which Quarkus Extension Should You Use? (Jib vs. Docker vs. Podman vs. Buildpack)

Quarkus provides four primary extensions to build container images. If you are unsure which one to pick for pushing to GKE, here is a quick guide:

### 1. `quarkus-container-image-jib` (🏆 Highly Recommended for most Java apps)
- **How it works:** Uses Google's Jib library to build OCI-compliant images purely in Java.
- **Pros:** Extremely fast. **NO Docker daemon needed** on your machine. Has excellent layering/caching (separates dependencies from your code). 
- **When to use:** Use this as your **default choice** unless you have a specific reason not to. It makes cross-platform builds (e.g., Mac `arm64` to GKE `amd64`) painless because it relies completely on Java rather than OS emulators.

### 2. `quarkus-container-image-docker`
- **How it works:** Uses your local Docker daemon and the `src/main/docker/Dockerfile...` generated by Quarkus.
- **Pros:** Fully customizable. You can freely edit the Dockerfile to add OS-level packages (like `apt-get install curl`). Uses `buildx` for multi-platform support.
- **When to use:** Use this if you have a custom `Dockerfile` or if your backend application requires specific Linux OS-level dependencies installed that Jib cannot provide out-of-the-box.

### 3. `quarkus-container-image-podman`
- **How it works:** Similar to the Docker extension, but uses Podman (which must be installed locally) to build from your Dockerfiles.
- **Pros:** Can build multi-platform images natively without needing external plugins. Runs rootless by default, adhering to stricter security protocols.
- **When to use:** Use this if your company has standardized on **Podman** over Docker.

### 4. `quarkus-container-image-buildpack`
- **How it works:** Uses Cloud Native Buildpacks (CNB) to analyze your app and automatically build a standardized image without explicitly needing a Dockerfile.
- **Pros:** Provides standardized, highly secure default base images curated by buildpack providers (like Paketo/Heroku).
- **When to use:** Use this if your organization mandates Cloud Native Buildpacks to standardize all container images across different languages (Node.js, Go, Java), or if you want a completely hands-off setup. *(Note: This extension still requires a Docker daemon running locally out of the box).*
