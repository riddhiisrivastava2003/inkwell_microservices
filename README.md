# 🖊️ Inkwell Blogging — Frontend

> A modern, feature-rich blogging platform frontend built with **React 19**, **Vite**, **React Router v7**, **Recharts**, **Framer Motion**, and **TipTap Editor**.

---

## 📑 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Project Structure](#project-structure)
- [Pages & Routes](#pages--routes)
- [User Roles & Access](#user-roles--access)
- [Key Features](#key-features)
- [API Integration](#api-integration)
- [Theming (Dark / Light Mode)](#theming-dark--light-mode)
- [Testing](#testing)
- [Build for Production](#build-for-production)
- [Backend Connection](#backend-connection)

---

## Overview

**Inkwell Frontend** is the React-based client for the Inkwell blogging platform. It connects to the backend microservices through the API Gateway at `http://localhost:8080`. The application supports three distinct user roles — **Reader**, **Author**, and **Admin** — each with dedicated dashboards and protected routes.

---

## Tech Stack

| Category | Technology |
|---|---|
| UI Library | React 19 |
| Build Tool | Vite 8 |
| Routing | React Router DOM v7 |
| Styling | Bootstrap 5.3 + React-Bootstrap + Custom CSS |
| Animations | Framer Motion 12 |
| Rich Text Editor | TipTap v3 |
| Charts & Analytics | Recharts 3 |
| HTTP Client | Axios |
| Notifications (Toast) | React Hot Toast |
| Date Formatting | date-fns |
| Icons | React Icons 5 |
| Loading Skeletons | React Loading Skeleton |
| Content Sanitisation | DOMPurify |
| Testing | Vitest + Testing Library |

---

## Prerequisites

- **Node.js** v18 or higher
- **npm** v9 or higher
- Inkwell **Backend** running at `http://localhost:8080` (API Gateway)

---

## Getting Started

### 1. Clone & Install

```bash
# Navigate to the frontend directory
cd inkwell-frontend

# Install dependencies
npm install
```

### 2. Setup Environment

```bash
# Copy the example env file
cp .env.example .env
```

Edit `.env` with your values (see [Environment Variables](#environment-variables)).

### 3. Start Development Server

```bash
npm run dev
```

The app will be available at: **http://localhost:5173**

---

## Environment Variables

Create a `.env` file in the `inkwell-frontend/` root:

```env
VITE_API_BASE_URL=http://localhost:8080
```

> The Vite dev server proxies all `/api-gw/*` requests to `http://localhost:8080`, so you don't need to set CORS headers during development.

---

## Project Structure

```
inkwell-frontend/
├── index.html                    # App entry HTML
├── vite.config.js                # Vite config (proxy setup)
├── vitest.config.js              # Test config
├── eslint.config.js              # ESLint rules
├── package.json
├── public/                       # Static assets
└── src/
    ├── main.jsx                  # React root entry
    ├── App.jsx                   # App shell (Router + Providers)
    ├── router.jsx                # All route definitions
    ├── index.css                 # Global base styles
    ├── App.css                   # App-level styles
    │
    ├── assets/                   # Images, icons, static files
    │
    ├── styles/
    │   └── theme.css             # CSS variables: dark/light theme tokens
    │
    ├── contexts/
    │   ├── AuthContext.jsx       # Auth state (user, token, login, logout)
    │   ├── ThemeContext.jsx      # Dark/Light mode toggle
    │   └── NotificationContext.jsx # In-app notification state
    │
    ├── hooks/                    # Custom React hooks
    │
    ├── layouts/
    │   ├── PublicLayout.jsx      # Layout for public pages (Navbar + Footer)
    │   ├── DashboardLayout.jsx   # Author dashboard layout (Sidebar)
    │   └── AdminLayout.jsx       # Admin panel layout (Sidebar)
    │
    ├── components/
    │   ├── common/
    │   │   ├── AppNavbar.jsx     # Top navigation bar
    │   │   └── AppFooter.jsx     # Site footer
    │   ├── dashboard/
    │   │   └── DashboardSidebar.jsx
    │   ├── post/
    │   │   └── PostCard.jsx      # Reusable blog post card
    │   ├── comments/             # Comment components
    │   ├── editor/               # TipTap rich text editor wrapper
    │   └── layout/               # Layout utilities
    │
    ├── pages/
    │   ├── public/               # Publicly accessible pages
    │   ├── auth/                 # Login, Register, Password Reset, OAuth
    │   ├── author/               # Author dashboard pages
    │   ├── admin/                # Admin panel pages
    │   ├── reader/               # Reader dashboard pages
    │   └── shared/               # Shared pages (404, ProtectedRoute)
    │
    ├── services/
    │   ├── authService.js        # Auth API calls
    │   ├── postService.js        # Post CRUD API calls
    │   ├── commentService.js     # Comment & like API calls
    │   ├── mediaService.js       # File upload API calls
    │   ├── newsletterService.js  # Newsletter API calls
    │   ├── notificationService.js # Notification API calls
    │   └── api/                  # Axios instance & interceptors
    │
    └── utils/                    # Helper utilities
```

---

## Pages & Routes

### 🌐 Public Routes

| Path | Page | Description |
|---|---|---|
| `/` | `HomePage` | Blog feed with all published posts |
| `/posts/:postId` | `PostDetailPage` | Single post view (by ID) |
| `/posts/slug/:slug` | `PostDetailPage` | Single post view (by slug) |
| `/search` | `SearchPage` | Search posts by keyword |
| `/category/:categoryId` | `CategoryTagPage` | Posts filtered by category |
| `/tag/:tagId` | `CategoryTagPage` | Posts filtered by tag |
| `/author/:authorId` | `AuthorProfilePage` | Public author profile |
| `/about` | `AboutPage` | About the platform |
| `/privacy` | `PrivacyPage` | Privacy policy |
| `/newsletter/confirm` | `NewsletterConfirmPage` | Email subscription confirmation |
| `/newsletter/unsubscribe` | `NewsletterUnsubscribePage` | Unsubscribe from newsletter |

### 🔐 Auth Routes

| Path | Page | Description |
|---|---|---|
| `/login` | `LoginPage` | Email/password login + OAuth |
| `/register` | `RegisterPage` | New user registration |
| `/register/admin` | `RegisterPage` (admin mode) | Admin registration with secret key |
| `/forgot-password` | `ForgotPasswordPage` | Request password reset email |
| `/reset-password` | `ResetPasswordPage` | Set new password via token |
| `/auth/oauth-success` | `OAuthSuccessPage` | OAuth2 redirect handler |

### 👤 Reader Routes _(Protected: READER, AUTHOR, ADMIN)_

| Path | Page | Description |
|---|---|---|
| `/reader-dashboard` | `ReaderDashboardPage` | Reader overview |
| `/reader/saved` | `SavedPostsPage` | Bookmarked posts |

### ✍️ Author Dashboard Routes _(Protected: AUTHOR, ADMIN)_

| Path | Page | Description |
|---|---|---|
| `/dashboard` | `AuthorDashboardPage` | Author overview & stats |
| `/dashboard/posts` | `MyPostsPage` | Manage own posts |
| `/dashboard/posts/new` | `CreateEditPostPage` | Create new post |
| `/dashboard/posts/:id/edit` | `CreateEditPostPage` | Edit existing post |
| `/dashboard/comments` | `AuthorCommentsPage` | View comments on own posts |
| `/dashboard/media` | `MediaLibraryPage` | Manage uploaded media |
| `/dashboard/analytics` | `AuthorAnalyticsPage` | Post performance analytics |

### 🛡️ Admin Panel Routes _(Protected: ADMIN only)_

| Path | Page | Description |
|---|---|---|
| `/admin` | `AdminDashboardPage` | Platform overview |
| `/admin/users` | `UserManagementPage` | Manage users & roles |
| `/admin/posts` | `PostModerationPage` | Review & moderate posts |
| `/admin/taxonomy` | `TaxonomyManagementPage` | Manage categories & tags |
| `/admin/comments` | `AdminCommentsPage` | Platform-wide comment management |
| `/admin/newsletter` | `NewsletterPage` | Send newsletters & manage subscribers |
| `/admin/media` | `AdminMediaPage` | Platform-wide media management |
| `/admin/analytics` | `PlatformAnalyticsPage` | Platform-level analytics & charts |
| `/admin/audit-logs` | `AuditLogsPage` | System audit trail |

---

## User Roles & Access

| Role | Can Do |
|---|---|
| **Guest** | Browse public posts, search, view author profiles |
| **READER** | + Save posts, manage profile, newsletter subscribe |
| **AUTHOR** | + Create/edit posts, upload media, view own analytics |
| **ADMIN** | Full platform control: users, posts, taxonomy, newsletter, audit logs |

Route access is enforced by the `ProtectedRoute` component using the `AuthContext`.

---

## Key Features

### 📝 Rich Text Editor
- TipTap v3 integration with full formatting: **bold**, *italic*, headings, lists, code blocks, blockquotes.
- Content is sanitised with **DOMPurify** before rendering.

### 📊 Analytics & Charts
- **Recharts** used for bar charts, area charts, and pie charts.
- Author analytics: post views, engagement over time.
- Platform analytics (admin): user growth, post trends.

### 🎨 Dark / Light Theme
- Toggle managed via `ThemeContext`.
- CSS custom properties (variables) in `theme.css` power all theme-aware styling.
- Theme preference persisted via `localStorage`.

### 🔔 Notifications
- In-app notifications via `NotificationContext`.
- Toast notifications via **React Hot Toast**.

### 📧 Newsletter
- Readers can subscribe/unsubscribe from newsletters.
- Admin can compose and send newsletter campaigns.

### 🔒 Authentication
- JWT-based session management via `AuthContext`.
- Google & GitHub OAuth2 social login.
- Password reset via email token flow.

### 🖼️ Media Library
- Upload images directly to the media service.
- Browse uploaded media in a visual library.

### ⚡ Performance
- **Loading skeletons** (`react-loading-skeleton`) for async data states.
- **Framer Motion** for smooth page transitions and micro-animations.

---

## API Integration

All API calls are centralised in `src/services/`. Each service file uses a shared **Axios instance** from `src/services/api/` which automatically attaches the JWT token from `localStorage`.

| Service File | Backend Service |
|---|---|
| `authService.js` | `auth-service` (port 8081) |
| `postService.js` | `post-category-tag-service` (port 8082) |
| `commentService.js` | `comment-like-service` (port 8083) |
| `mediaService.js` | `media-service` (port 8084) |
| `newsletterService.js` | `newsletter-subscription-service` (port 8085) |
| `notificationService.js` | `notification-service` (port 8086) |

All requests are routed through **API Gateway** at `http://localhost:8080` via Vite's dev proxy:

```js
// vite.config.js proxy
'/api-gw' → 'http://localhost:8080'
```

---

## Theming (Dark / Light Mode)

The theme system uses CSS custom properties defined in `src/styles/theme.css`.

```css
/* Example theme tokens */
--bg-primary
--bg-secondary
--text-primary
--text-secondary
--accent-color
--border-color
```

Toggle dark/light mode via the navbar toggle — preference is saved in `localStorage` and restored on page load through `ThemeContext`.

---

## Testing

Tests are written with **Vitest** and **Testing Library**.

### Run all tests:

```bash
npm run test
```

### Run in watch mode:

```bash
npm run test:watch
```

### Open Vitest UI:

```bash
npm run test:ui
```

### View coverage:

```bash
npx vitest run --coverage
```

Coverage reports are saved to `coverage/`.

---

## Build for Production

```bash
npm run build
```

Output is placed in `dist/`. Preview the production build locally:

```bash
npm run preview
```

---

## Backend Connection

The frontend expects the Inkwell backend API Gateway running at:

```
http://localhost:8080
```

Make sure you have started all backend services before running the frontend. See the **[Backend README](../inkwell-blogging/README.md)** for full setup instructions.

### Quick backend startup summary:

```
1. docker-compose up -d          (from inkwell-blogging/)
2. Start: eureka-server
3. Start: all microservices
4. Start: api-gateway
5. npm run dev                   (from inkwell-frontend/)
```

---


