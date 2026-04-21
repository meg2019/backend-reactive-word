# Getting Started with Google Cloud Platform (GCP): A Guide for New Engineers

Welcome to the team! As an engineer working with Google Cloud Platform (GCP), one of your first steps is to familiarize yourself with the environment. Google generously offers a **$300 free trial credit** for new users to explore and build on GCP. 

This guide will walk you through the process of creating your personal GCP account and claiming your $300 grant so you can start gaining hands-on experience safely.

## Table of Contents
* [Prerequisites](#prerequisites)
* [Step-by-Step Guide to Claim Your $300 Credits](#step-by-step-guide-to-claim-your-300-credits)
  * [Step 1: Navigate to the GCP Free Trial Page](#step-1-navigate-to-the-gcp-free-trial-page)
  * [Step 2: Start the Free Trial](#step-2-start-the-free-trial)
  * [Step 3: Sign In](#step-3-sign-in)
  * [Step 4: Complete Your Profile](#step-4-complete-your-profile)
  * [Step 5: Add Payment Verification](#step-5-add-payment-verification)
  * [Step 6: Begin Exploring!](#step-6-begin-exploring)
* [Understanding the Free Tier vs. Free Trial](#understanding-the-free-tier-vs-free-trial)
* [Bonus Step: Pushing Local Docker Images for Google Kubernetes Engine (GKE)](#bonus-step-pushing-local-docker-images-for-google-kubernetes-engine-gke)
  * [Step 1: Install the gcloud CLI](#step-1-install-the-gcloud-cli)
  * [Step 1.5: Authenticate the gcloud CLI](#step-15-authenticate-the-gcloud-cli)
  * [Step 2: Enable the Artifact Registry API](#step-2-enable-the-artifact-registry-api)
  * [Step 3: Create an Artifact Registry Repository](#step-3-create-an-artifact-registry-repository)
  * [Step 4: Map Docker to Authenticate with GCP](#step-4-map-docker-to-authenticate-with-gcp)
  * [Step 5: Tag Your Local Docker Image](#step-5-tag-your-local-docker-image)
  * [Step 6: Push the Image](#step-6-push-the-image)
* [What Happens After the Trial?](#what-happens-after-the-trial)

## Prerequisites

Before you begin, ensure you have the following ready:
1. **Google Account:** You must have a Gmail or Google-affiliated account. If you don't have one, you will need to create it first.
2. **Payment Method:** You will need a valid credit or debit card. **Don't worry, you won't be charged.** Google uses this merely to verify your identity and differentiate you from a robot to prevent abuse.
3. **New Customer Eligibility:** The free trial is for new users only. You cannot have been a previously paying customer of Google Cloud, Google Maps Platform, or Firebase. 

## Step-by-Step Guide to Claim Your $300 Credits

### Step 1: Navigate to the GCP Free Trial Page
Head over to the official Google Cloud Free Trial page by clicking here: [Google Cloud Free Trial](https://cloud.google.com/free/).

### Step 2: Start the Free Trial
Click on the **"Get started for free"** or **"Start free"** button displayed prominently on the page.

### Step 3: Sign In
You will be prompted to sign in. Enter the credentials of the Google Account you wish to associate with your GCP environment.

### Step 4: Complete Your Profile
Follow the on-screen prompts to set up your billing account:
*   **Country:** Select your country of residence carefully, as this dictates the currency and billing regulations.
*   **Describe your organization/needs:** Answer the onboarding questions. Usually, selecting options like "Personal Project" or "Student" works well for getting started.
*   **Terms of Service:** Read and agree to the Terms of Service.

### Step 5: Add Payment Verification
Enter your credit or debit card details, along with your billing address.
> **Important Note:** Google may make a temporary, refundable micro-charge (like $1) to verify your card is active. **You will not be automatically billed when the trial ends.**

### Step 6: Begin Exploring!
Once your payment method is verified and the profile is complete, you will be redirected to the **Google Cloud Console**. Congratulations! 

You will now automatically have **$300** in credit applied to your Cloud Billing account, which is valid for **90 days**.

## Understanding the Free Tier vs. Free Trial

It's important to differentiate between the Free Trial ($300 credit) and the Free Tier that GCP offers:

1. **The $300 Free Trial:** This is a grant giving you $300 in credits to spend on almost *any* GCP service (with a few exceptions like cryptocurrency mining). It expires after 90 days or when you run out of credits, whichever comes first.

2. **The Always Free Tier:** Many GCP products, such as Compute Engine, Cloud Storage, and Cloud Run, offer a "Free Tier." This provides you with specific monthly usage limits that are completely free of charge, *even after your $300 trial ends or expires*. (e.g., 1 non-preemptible e2-micro VM instance per month).

## Bonus Step: Pushing Local Docker Images for Google Kubernetes Engine (GKE)

If you are following tutorials for GKE, you will often need to push your local Docker images to GCP so that your Kubernetes clusters can pull them. GCP uses **Artifact Registry** as its next-generation service to store, manage, and secure your container images (replacing the older Container Registry).

Here is a step-by-step guide to push a local Docker image to Artifact Registry within your free trial project:

### Step 1: Install the gcloud CLI

Depending on your operating system, follow the steps below to install the Google Cloud CLI (`gcloud`).

#### For Mac mini M4 (Apple Silicon)
The quickest way to install the CLI on a Mac with an M4 chip (ARM64) is using Homebrew, or via the official package:
*   **Via Homebrew:** Open your terminal and run:
    ```bash
    brew install --cask google-cloud-sdk
    ```
*   **Via Official Archive:** 
    1. Download the Darwin ARM64 archive:
       ```bash
       curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-darwin-arm.tar.gz
       ```
    2. Extract and run the installer:
       ```bash
       tar -xf google-cloud-cli-darwin-arm.tar.gz
       ./google-cloud-sdk/install.sh
       ```

#### For Windows 11
1. Open a new PowerShell window.
2. Run the following command to download and launch the installer:
    ```powershell
    (New-Object Net.WebClient).DownloadFile("https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe", "$env:Temp\GoogleCloudSDKInstaller.exe")
    & $env:Temp\GoogleCloudSDKInstaller.exe
    ```
3. Follow the graphical setup wizard. Keep the options to start the Google Cloud CLI SDK Shell and run `gcloud init` checked.

#### For Linux Mint (Debian-based)
Since Linux Mint is based on Ubuntu/Debian, you can install the CLI using the `apt` package manager:
1. Update your packages and install prerequisites:
    ```bash
    sudo apt-get update
    sudo apt-get install apt-transport-https ca-certificates gnupg curl
    ```
2. Import the Google Cloud public key:
    ```bash
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
    ```
3. Add the gcloud CLI distribution URI as a package source:
    ```bash
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
    ```
4. Update and install:
    ```bash
    sudo apt-get update && sudo apt-get install google-cloud-cli
    ```

### Step 1.5: Authenticate the gcloud CLI
Once installed, open your shell/terminal and run the following to securely link your local computer to your GCP Account:
```bash
# Log in to your Google Account (opens a browser window)
gcloud auth login

# Set your current GCP Project (you can find your Project ID in the GCP console)
gcloud config set project YOUR_PROJECT_ID
```

### Step 2: Enable the Artifact Registry API
Before using Artifact Registry, you need to enable its API for your project:
```bash
gcloud services enable artifactregistry.googleapis.com
```

### Step 3: Create an Artifact Registry Repository
Create a Docker repository to hold your images. Replace `us-central1` with your preferred region and `my-docker-repo` with your desired repository name:
```bash
gcloud artifacts repositories create my-docker-repo \
    --repository-format=docker \
    --location=us-central1 \
    --description="Docker repository for GKE testing"
```

### Step 4: Map Docker to Authenticate with GCP
Configure your local Docker installation to authenticate with Google Artifact Registry for your specific region so you can push and pull securely:
```bash
# Replace 'us-central1' with the same region used in Step 3
gcloud auth configure-docker us-central1-docker.pkg.dev
```

### Step 5: Tag Your Local Docker Image
To push an image to a registry, you must tag it with the registry's location. The format is: `[REGION]-docker.pkg.dev/[PROJECT_ID]/[REPO_NAME]/[IMAGE_NAME]:[TAG]`

Assuming your local image is named `my-app:v1` (replace it with your actual local image name):
```bash
docker tag my-app:v1 us-central1-docker.pkg.dev/YOUR_PROJECT_ID/my-docker-repo/my-app:v1
```

### Step 6: Push the Image
Finally, use standard Docker commands to push the image to the remote registry:
```bash
docker push us-central1-docker.pkg.dev/YOUR_PROJECT_ID/my-docker-repo/my-app:v1
```

Success! Your image is now safely hosted on GCP, and you can reference this exact image URL (`us-central1-docker.pkg.dev/...`) in your Kubernetes deployment YAML files when spinning up workloads on GKE.

## What Happens After the Trial?

When your $300 credit is exhausted or the 90-day period ends:
*   Your services and projects will be paused/suspended.
*   **You will NOT be automatically billed.** 
*   To continue using GCP and avoid losing your data, you will need to manually upgrade to a paid billing account in the Cloud Console.

Good luck, and have fun exploring GCP services! Feel free to reach out to the broader engineering team if you run into any issues during your setup or when spinning up your first resources.
