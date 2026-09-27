

````markdown
# EventHandling

EventHandling is a college event management and discovery platform designed to help colleges create, manage, verify, and discover events.

The system consists of a web application, REST API backend, PostgreSQL database, and a mobile application.

---

## Tech Stack

### Website
- React
- JavaScript
- HTML5
- CSS

### Backend
- Kotlin
- Ktor
- REST API

### Database
- PostgreSQL

### Mobile Application
- Flutter
- Dart

### Development Tools
- Git
- GitHub
- Visual Studio Code
- Postman (for API testing)

---

## Project Structure

```text
EventHandling/
│
├── Website/              # React web application
│
├── backend/              # Kotlin + Ktor REST API
│
├── mobile/               # Flutter mobile application
│
├── database/             # PostgreSQL database scripts
│   ├── schema.sql        # Creates database tables
│   └── seed.sql          # Inserts initial data
│
└── README.md             # Project and setup documentation
````

---

# Database Setup

Each team member uses their own local PostgreSQL database during development.

The actual database is **not stored on GitHub**.

GitHub stores the SQL scripts required to recreate the database.

## Requirements

Install:

* PostgreSQL
* pgAdmin 4 (optional)
* Git

Make sure PostgreSQL's `bin` directory is available in the system PATH if you want to use `psql` from the terminal.

---

## 1. Create the Database

Open PostgreSQL/psql and create a database named:

```sql
CREATE DATABASE eventhandling;
```

Then connect to it:

```text
\c eventhandling
```

---

## 2. Create the Tables

From the project root, run:

```powershell
psql -U postgres -d eventhandling -f database/schema.sql
```

This creates all required tables and relationships.

---

## 3. Insert Initial Data

Run:

```powershell
psql -U postgres -d eventhandling -f database/seed.sql
```

This inserts initial:

* Colleges
* Departments
* Event Categories

---

## Database Tables

The current database contains:

1. `users`
2. `colleges`
3. `departments`
4. `events`
5. `event_categories`
6. `saved_events`
7. `event_reviews`
8. `verification_requests`

---

# Backend Setup

The backend is built using Kotlin and Ktor.

## Requirements

Install:

* JDK 21
* Git

The project uses the Gradle Wrapper, so a separate Gradle installation is not required.

---

## PostgreSQL Password

The backend uses the `DATABASE_PASSWORD` environment variable for the PostgreSQL password.

Do **not** put the PostgreSQL password directly into GitHub files.

For Windows PowerShell, the environment variable can be set with:

```powershell
$env:DATABASE_PASSWORD = "your_password"
```

The actual password should never be committed to the repository.

---

## Run the Backend

Open a terminal inside the `backend` folder:

```powershell
cd backend
```

Then run:

```powershell
.\gradlew.bat run
```

The backend runs at:

```text
http://localhost:8080
```

---

# Website Setup

The website is built using React.

## Requirements

Install:

* Node.js
* npm

Open a terminal inside the `Website` folder:

```powershell
cd Website
```

Install dependencies:

```powershell
npm install
```

Start the development server:

```powershell
npm start
```

The website normally runs at:

```text
http://localhost:3000
```

---

# Mobile Application

The mobile application will be developed using Flutter and Dart.

The mobile application will primarily focus on:

* Login
* Event discovery
* Search
* Filters
* Event details
* Saving events
* User profile

Administrative and complex event-management functionality will primarily remain on the website.

---

# User Roles

The system uses a single `users` table.

Roles are stored in the `role` column.

Current roles:

* `PLATFORM_ADMIN`
* `COLLEGE_ADMIN`
* `EVENT_COORDINATOR`
* `FACULTY_COORDINATOR`
* `STUDENT_ORGANIZER`
* `NORMAL_USER`

Designation is stored separately.

For example:

```text
Role: COLLEGE_ADMIN
Designation: Principal
```

or:

```text
Role: FACULTY_COORDINATOR
Designation: HOD - AIML
```

This avoids creating separate database tables for every designation.

---

# Event Workflow

The basic event workflow is:

```text
Create Event
     ↓
Draft
     ↓
Submit for Approval
     ↓
Pending Review
     ↓
Approved / Rejected
     ↓
Published
```

Events have a permanent `event_id`.

Editing an event updates the existing event instead of creating a new event.

---

# Event Visibility

Users normally see events from their own college first.

Users can also explore events from another college.

External users will only see events where:

```text
Accept Visitors = Yes
```

---

# Venue and Location

Events support two venue types:

```text
COLLEGE_CAMPUS
EXTERNAL_VENUE
```

For college-campus events, the college's stored address and coordinates can be used.

For external venues, the system will store the venue address and coordinates.

Latitude and longitude are system-generated and are not manually entered by the user.

These coordinates will later support distance-based event discovery.

---

# Event Features

Planned event features include:

* Event discovery
* Search
* Filtering
* Sorting
* Event verification
* Event ratings/reviews
* Save event
* Copy event link
* Event rules
* Venue/location information
* College-based event discovery

Event registration/booking is **not part of the current system design**.

---

# Git and GitHub Workflow

The `main` branch should contain stable code.

For new work, create a feature branch.

Example:

```powershell
git checkout main
git pull
git checkout -b feature-name
```

After making changes:

```powershell
git add .
git commit -m "Describe what changed"
git push -u origin feature-name
```

Then create a Pull Request on GitHub and merge it into `main` after review.

---

# Important Git Rules

### Do

* Pull the latest `main` before starting new work.
* Use feature branches.
* Make meaningful commits.
* Review changes before committing.
* Keep the project structure organized.

### Do Not

* Commit passwords.
* Commit API keys.
* Commit authentication tokens.
* Commit local database files.
* Commit `node_modules`.
* Commit generated build files.
* Work directly on `main` unless necessary.

---

# Database Team Workflow

Each developer has their own local PostgreSQL database.

```text
                GitHub
                   │
       ┌───────────┼───────────┐
       ↓           ↓           ↓
    Abhi DB    Praniti DB   Anshuli DB
       │           │           │
       └────── Same SQL ───────┘
          schema.sql + seed.sql
```

The database itself is not shared through GitHub.

If the database structure changes:

1. Update `database/schema.sql`.
2. Update `database/seed.sql` if required.
3. Commit the SQL changes.
4. Push them to GitHub.
5. Other team members pull the changes.
6. They update/recreate their local database as required.

---

# Current Development Status

### Completed

* GitHub repository setup
* React website setup
* Kotlin + Ktor backend setup
* PostgreSQL setup
* Database schema
* Database seed data
* Basic PostgreSQL connection from backend

### In Progress

* Backend REST API
* Authentication
* User management
* Event management
* Role-based authorization

### Later

* Flutter mobile application
* Event discovery
* Search and filters
* Event reviews
* Location-based discovery
* Final deployment

---

# Team

EventHandling is developed as a team project.

All contributors should use Git branches and Pull Requests to keep the `main` branch stable.

---

## Note

This project is currently under development. Some features documented as planned may not yet be implemented.

```