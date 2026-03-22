# Real Estate Agent Finder

A Spring Boot MVC web application for finding, rating, and booking appointments with real estate agents. Data is persisted using flat text files (no database required).

## Features
- Browse and search real estate agents
- Agent profiles with ratings and reviews
- Book, reschedule, and cancel appointments
- Top-rated agents sorted via Selection Sort
- BST-based fast agent lookup by ID
- Admin panel for managing users, agents, and admins

## Tech Stack
- **Backend**: Java 17, Spring Boot 3.2, Spring MVC
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Data Storage**: Flat text files (CSV format)
- **Data Structures**: BST (agent lookup), Selection Sort (rankings)

## Project Structure
```
src/main/java/com/realestate/app/
├── controller/   MVC controllers
├── model/        Domain models (Person, Agent, User, Appointment, Rating, Review)
├── service/      Business logic layer
├── util/         FileHandler, IdGenerator, AgentBST, SelectionSortUtil
├── dto/          Form binding objects
└── config/       Spring MVC configuration
```
## Educational Client Project

This system was developed as part of an academic client-based project  
under the Software Engineering degree program at SLIIT.

The implementation reflects student design decisions, software  
engineering practices, and technical experimentation for educational  
purposes.

The project is shared publicly for portfolio demonstration and  
knowledge sharing only.

All trademarks, business names, or sample data used in this project  
are for academic simulation purposes.

## Running the Application
```bash
mvn spring-boot:run
```
Then open `http://localhost:8080` in your browser.

## Data Files
All data is stored in `src/main/resources/data/` as plain text CSV files:
- `admins.txt`, `agents.txt`, `users.txt`
- `appointments.txt`, `ratings.txt`, `reviews.txt`
