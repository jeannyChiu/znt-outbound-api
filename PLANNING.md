# PLANNING.md

## Project Vision

### Mission Statement
Build a robust, enterprise-grade integration platform that seamlessly connects Zenitron (增你強) internal systems with JIT (恩及通) Warehouse Management System, enabling automated warehouse operations, real-time data synchronization, and efficient inventory management across multiple warehouse zones.

### Strategic Goals
- **Digital Transformation**: Automate manual warehouse processes and eliminate data silos
- **Operational Excellence**: Achieve <3 second API response times and 99.9% system availability
- **Scalability**: Support multiple warehouse zones and extensible for future logistics providers
- **Data Integrity**: Ensure real-time, accurate inventory tracking with comprehensive audit trails
- **Business Continuity**: Implement robust error handling, retry mechanisms, and graceful degradation

### Success Metrics
- **Performance**: API response time <3 seconds, 99.9% uptime
- **Reliability**: <0.1% error rate for critical operations
- **Efficiency**: 80% reduction in manual warehouse operations
- **Scalability**: Support for concurrent processing and batch operations
- **Maintainability**: Comprehensive test coverage and modular architecture

## System Architecture

### High-Level Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    Zenitron Internal Systems                │
├─────────────────────────────────────────────────────────────┤
│                    Spring Boot Integration Layer             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   應用層        │  │   服務層        │  │   基礎層        │ │
│  │  (業務邏輯)     │  │  (API封裝)      │  │  (HTTP/認證)    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    JIT WMS System APIs                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │     認證        │  │    入庫管理     │  │    庫存操作     │ │
│  │   (Auth)        │  │    (ASN)        │  │  (Move/Trade)   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Core Architecture Layers

**1. Application Layer (應用層)**
- Business logic and workflow orchestration
- Transaction management and data validation
- Scheduled task coordination and monitoring
- Error handling and notification services

**2. Service Layer (服務層)**
- JIT API client abstraction and wrapping
- Data mapping and transformation services
- Configuration management and dynamic loading
- Caching and performance optimization

**3. Infrastructure Layer (基礎層)**
- HTTP client and authentication management
- Database connectivity and connection pooling
- Logging, monitoring, and observability
- Security and encryption services

### Key Architectural Patterns

**Configuration-Driven Design**
- Database-stored configuration for all API endpoints
- Environment-specific settings without code changes
- Runtime configuration updates without redeployment

**Event-Driven Processing**
- Scheduled task execution with comprehensive monitoring
- Asynchronous processing for non-critical operations
- Event logging and audit trail generation

**Resilience Patterns**
- Circuit breaker for external API calls
- Retry with exponential backoff
- Graceful degradation and fallback mechanisms
- Dead letter queue for failed operations

## Technology Stack

### Core Framework
- **Spring Boot 3.5.0**: Enterprise application framework
- **Java 17**: Modern LTS Java version with enhanced performance
- **Maven**: Build automation and dependency management
- **Lombok**: Code generation for reduced boilerplate

### Database & Persistence
- **Oracle Database**: Enterprise-grade relational database
- **Spring Data JPA**: Object-relational mapping and data access
- **HikariCP**: High-performance JDBC connection pooling
- **Database**: B2BDB instance on 10.1.1.15:1521

### Communication & Integration
- **Spring Web**: RESTful web services and HTTP client
- **RestTemplate**: Synchronous HTTP client for JIT API integration
- **Jackson**: JSON serialization and deserialization
- **HTTPS/TLS**: Encrypted communication protocols

### Monitoring & Operations
- **SLF4J + Logback**: Comprehensive logging framework
- **Spring Actuator**: Application monitoring and management endpoints
- **Spring Mail**: Email notification services
- **Custom Metrics**: Performance and business metrics tracking

### Security
- **Token-based Authentication**: Secure API access with automatic refresh
- **HTTPS Enforcement**: Encrypted data transmission
- **Database Credential Management**: Secure configuration storage
- **Audit Logging**: Comprehensive security event tracking

### Development & Testing
- **Spring Boot Test**: Testing framework and utilities
- **JUnit 5**: Unit testing framework
- **Maven Surefire**: Test execution and reporting
- **Integration Testing**: End-to-end API testing capabilities

## Required Tools & Environment

### Development Environment
```bash
# Core Requirements
Java 17 JDK (OpenJDK or Oracle JDK)
Apache Maven 3.8+
IDE: IntelliJ IDEA or Eclipse with Spring Tools

# Database Access
Oracle Database Client (SQL Developer or similar)
Oracle JDBC Driver (included in project dependencies)

# Version Control
Git 2.30+
GitHub Desktop or Git CLI
```

### Development Tools
```bash
# API Testing
curl or Postman for API testing
REST client for endpoint validation

# Database Tools
Oracle SQL Developer or DBeaver
Database schema comparison tools

# Build & Deployment
Maven (included wrapper: ./mvnw)
Docker (optional for containerization)
```

### Runtime Environment
```bash
# Production Server Requirements
Java 17 Runtime Environment
Oracle Database connectivity
HTTPS/TLS certificate management
Log rotation and monitoring tools

# Network Requirements
HTTPS access to JIT APIs (gt-test.njtsys.com)
Oracle database connectivity (10.1.1.15:1521)
SMTP server access (tp.zenitron.com.tw:25)
```

### Monitoring & Operations Tools
```bash
# Application Monitoring
Spring Boot Actuator endpoints
Custom health checks and metrics
Log aggregation and analysis tools

# Database Monitoring
Oracle Enterprise Manager or equivalent
Connection pool monitoring
Query performance analysis

# Infrastructure Monitoring
Server resource monitoring (CPU, memory, disk)
Network connectivity monitoring
SSL certificate expiry monitoring
```

### Development Workflow Tools
```bash
# Code Quality
Maven plugins for code analysis
Lombok annotation processing
Spring Boot DevTools for hot reload

# Testing & Validation
JUnit 5 test framework
Maven test reporting
Integration test environment setup

# Documentation
Javadoc generation
API documentation tools
Database schema documentation
```

### Configuration Management
```bash
# Environment Configuration
application.properties for different environments
Database configuration management
External configuration management

# Security Management
Credential rotation procedures
SSL certificate management
API token lifecycle management
```

## Key Integration Points

### JIT WMS API Endpoints
- **Authentication**: `/comm/auth/api-login`
- **ASN Management**: `/wms/b2b-api/create-asn-by-api`
- **Inventory Operations**: `/project/b2b-api/inv-move-or-trade`
- **Inventory Exchange**: `/project/b2b-api/inv-exchange-sku`
- **Inventory Query**: `/project/b2b-api/get-inv-loc-list`

### Database Integration
- **Configuration Table**: `ZEN_B2B_TAB_D` for dynamic API configuration
- **Business Tables**: Integration with existing Zenitron database schema
- **Audit Tables**: Transaction logging and audit trail storage

### External Systems
- **Email Notifications**: Integration with Zenitron SMTP server
- **Monitoring Systems**: Integration with existing monitoring infrastructure
- **Backup Systems**: Database backup and recovery procedures

## Performance & Scalability Requirements

### Performance Targets
- **API Response Time**: <3 seconds for all operations
- **Throughput**: Support for 100+ concurrent API calls
- **Batch Processing**: Handle bulk operations efficiently
- **Database Performance**: Optimized queries and connection pooling

### Scalability Considerations
- **Horizontal Scaling**: Stateless application design for load balancing
- **Caching Strategy**: Token caching and configuration caching
- **Resource Management**: Efficient memory and connection utilization
- **Auto-scaling**: Support for dynamic resource allocation

### Reliability Requirements
- **Availability**: 99.9% uptime target
- **Error Handling**: Comprehensive exception handling and recovery
- **Data Consistency**: Transactional integrity and rollback capabilities
- **Monitoring**: Proactive alerting and health monitoring

This planning document serves as the foundation for all development activities and should be referenced for architectural decisions, technology choices, and implementation strategies.