<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AURA PRIVÉ | Private Access Gate</title>
    <link rel="stylesheet" href="css/luxury-style.css" />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap"
      rel="stylesheet"
    />
    <style>
      .auth-wrapper {
        min-height: 85vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 2rem;
      }
      .auth-card {
        max-width: 480px;
        width: 100%;
      }
      .mode-switch {
        display: flex;
        border-bottom: 1px solid var(--border-subtle);
        margin-bottom: 1.5rem;
        gap: 1.5rem;
      }
      .mode-link {
        background: none;
        border: none;
        padding: 0.6rem 0;
        font-size: 0.8rem;
        text-transform: uppercase;
        letter-spacing: 0.16em;
        color: var(--text-muted);
        cursor: pointer;
        font-weight: 600;
        border-bottom: 2px solid transparent;
        transition: all 0.25s ease;
      }
      .mode-link.active {
        color: var(--gold-accent);
        border-bottom-color: var(--gold-accent);
      }
      .role-selector {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.6rem;
        margin-top: 0.4rem;
      }
      .role-choice {
        padding: 0.75rem;
        text-align: center;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.13em;
        border: 1px solid var(--border-subtle);
        border-radius: 8px;
        background: oklch(14% 0.01 260 / 0.6);
        color: var(--text-muted);
        cursor: pointer;
        font-weight: 600;
        transition: all 0.25s ease;
      }
      .role-choice.active {
        background: linear-gradient(
          135deg in oklch,
          oklch(78% 0.15 80),
          oklch(88% 0.15 86)
        );
        color: oklch(13% 0.02 260);
        border-color: var(--gold-hover);
      }
      .link-btn {
        background: none;
        border: none;
        color: var(--gold-accent);
        font-size: 0.78rem;
        cursor: pointer;
        text-decoration: underline;
        padding: 0;
      }
      .modal-bg {
        display: none;
        position: fixed;
        inset: 0;
        background: oklch(8% 0.01 260 / 0.84);
        backdrop-filter: blur(14px);
        align-items: center;
        justify-content: center;
        z-index: 200;
        padding: 1.5rem;
      }
      .modal-bg.open {
        display: flex;
      }
      .status-banner {
        display: none;
        padding: 0.85rem;
        margin-bottom: 1.2rem;
        border-radius: 8px;
        font-size: 0.84rem;
      }
    </style>
  </head>
  <body>
    <nav class="navbar">
      <div class="brand-logo">AURA PRIVÉ</div>
      <div class="nav-links">
        <a href="index.jsp">Sanctuary Home</a>
      </div>
    </nav>

    <main class="auth-wrapper">
      <div class="auth-card">
        <div class="mode-switch">
          <button
            type="button"
            class="mode-link active"
            id="modeLoginBtn"
            onclick="setAuthMode('LOGIN')"
          >
            Log In
          </button>
          <button
            type="button"
            class="mode-link"
            id="modeSignupBtn"
            onclick="setAuthMode('SIGNUP')"
          >
            Create Account (Sign Up)
          </button>
        </div>

        <span class="badge-tag" id="portalBadge"
          >Unified Sovereign Access Gate</span
        >
        <h2
          id="portalHeading"
          style="font-family: var(--font-heading); margin-bottom: 1.2rem"
        >
          Account Sign In
        </h2>

        <!-- Single clean banner populated only when an error or message actually exists -->
        <div id="statusBanner" class="status-banner"></div>

        <!-- 1. UNIFIED LOG IN FORM (Automatically routes to Client, Vendor, or Admin Portal) -->
        <form action="auth" method="POST" id="loginForm">
          <input type="hidden" name="action" value="login" />
          <div class="form-group">
            <label for="email">Registered Email ID</label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="Enter your email ID"
              required
            />
          </div>
          <div class="form-group">
            <label for="password">Security Passcode</label>
            <input
              type="password"
              id="password"
              name="password"
              placeholder="••••••"
              required
            />
          </div>
          <button
            type="submit"
            class="cta-button"
            style="width: 100%; margin-top: 0.5rem"
          >
            Authenticate & Enter Portal
          </button>

          <div
            style="
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-top: 1.2rem;
            "
          >
            <button
              type="button"
              class="link-btn"
              onclick="
                document.getElementById('resetModal').classList.add('open')
              "
            >
              Change Account Passcode
            </button>
            <span style="font-size: 0.74rem; color: var(--text-muted)"
              >SHA-256 Encrypted</span
            >
          </div>
        </form>

        <!-- 2. SIGN UP FORM (User chooses "You are: Client / Vendor" and opens their dedicated portal) -->
        <form
          action="auth"
          method="POST"
          id="signupForm"
          style="display: none"
          onsubmit="return validateSignup();"
        >
          <input type="hidden" name="action" value="signup" />
          <input type="hidden" name="role" id="signupRole" value="CLIENT" />

          <div class="form-group">
            <label>You Are Registering As</label>
            <div class="role-selector">
              <button
                type="button"
                class="role-choice active"
                id="chooseClient"
                onclick="selectSignupRole('CLIENT')"
              >
                I am a Client
              </button>
              <button
                type="button"
                class="role-choice"
                id="chooseVendor"
                onclick="selectSignupRole('VENDOR')"
              >
                I am a Vendor
              </button>
            </div>
          </div>

          <div class="form-group">
            <label id="nameLabel">Client Full Name</label>
            <input
              type="text"
              id="fullName"
              name="fullName"
              placeholder="e.g., Kunwar Devvrat Singhania"
              required
            />
          </div>
          <div class="form-group" id="clientTierGroup">
            <label>Client Membership Tier</label>
            <select name="membershipTier">
              <option value="Sovereign Founders Circle">
                Sovereign Founders Circle
              </option>
              <option value="Diplomatic Black Tier">
                Diplomatic Black Tier
              </option>
              <option value="Apex Syndicate">Apex Syndicate</option>
            </select>
          </div>
          <div class="form-group" id="vendorSpecGroup" style="display: none">
            <label>Vendor Service Specialty</label>
            <input
              type="text"
              name="specialty"
              placeholder="e.g., Royal Heritage Catering & Security"
            />
          </div>
          <div class="form-group">
            <label>Email ID (For Login)</label>
            <input
              type="email"
              id="signupEmail"
              name="email"
              placeholder="yourname@pro.abc"
              required
            />
          </div>
          <div class="form-group">
            <label>Create Passcode (Min 6 Characters)</label>
            <input
              type="password"
              id="signupPass"
              name="password"
              minlength="6"
              placeholder="••••••"
              required
            />
          </div>
          <div class="form-group">
            <label>Confirm Passcode</label>
            <input
              type="password"
              id="signupConfirm"
              name="confirmPassword"
              minlength="6"
              placeholder="••••••"
              required
            />
          </div>
          <button
            type="submit"
            class="cta-button"
            id="signupSubmitBtn"
            style="width: 100%; margin-top: 0.5rem"
          >
            Create Client Account & Open Portal
          </button>
        </form>
      </div>
    </main>

    <!-- VERIFIED PASSWORD CHANGE MODAL -->
    <div class="modal-bg" id="resetModal">
      <div class="auth-card">
        <span class="badge-tag">Zero-Trust Credential Security</span>
        <h3 style="font-family: var(--font-heading); margin-bottom: 1rem">
          Change Account Passcode
        </h3>

        <form action="auth" method="POST" onsubmit="return validateReset();">
          <input type="hidden" name="action" value="resetPassword" />

          <div class="form-group">
            <label>Registered Email ID</label>
            <input
              type="email"
              name="email"
              placeholder="Enter your registered email"
              required
            />
          </div>
          <div class="form-group">
            <label>Current (Old) Password</label>
            <input
              type="password"
              name="oldPassword"
              placeholder="Enter current password"
              required
            />
          </div>
          <div class="form-group">
            <label>New Password</label>
            <input
              type="password"
              id="newPass"
              name="newPassword"
              minlength="6"
              placeholder="Minimum 6 characters"
              required
            />
          </div>
          <div class="form-group">
            <label>Confirm New Password</label>
            <input
              type="password"
              id="confirmPass"
              name="confirmPassword"
              minlength="6"
              placeholder="Re-enter new password"
              required
            />
          </div>

          <div style="display: flex; gap: 0.8rem; margin-top: 1rem">
            <button type="submit" class="cta-button" style="flex: 1">
              Update Password
            </button>
            <button
              type="button"
              class="btn-small btn-danger"
              onclick="
                document.getElementById('resetModal').classList.remove('open')
              "
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>

    <script>
      // Render URL ?err= or ?msg= cleanly without ever showing empty boxes
      (function () {
        const params = new URLSearchParams(window.location.search);
        const banner = document.getElementById("statusBanner");
        if (params.get("err")) {
          banner.style.display = "block";
          banner.style.border = "1px solid var(--danger-accent)";
          banner.style.color = "var(--danger-accent)";
          banner.textContent = params.get("err");
        } else if (params.get("msg")) {
          banner.style.display = "block";
          banner.style.border = "1px solid var(--gold-accent)";
          banner.style.color = "var(--gold-accent)";
          banner.textContent = params.get("msg");
        }
      })();

      function setAuthMode(mode) {
        document
          .getElementById("modeLoginBtn")
          .classList.toggle("active", mode === "LOGIN");
        document
          .getElementById("modeSignupBtn")
          .classList.toggle("active", mode === "SIGNUP");
        document.getElementById("loginForm").style.display =
          mode === "LOGIN" ? "block" : "none";
        document.getElementById("signupForm").style.display =
          mode === "SIGNUP" ? "block" : "none";
        document.getElementById("portalBadge").textContent =
          mode === "LOGIN"
            ? "Unified Sovereign Access Gate"
            : "New Member Onboarding";
        document.getElementById("portalHeading").textContent =
          mode === "LOGIN" ? "Account Sign In" : "Create Your Account";
      }

      function selectSignupRole(role) {
        const isClient = role === "CLIENT";
        document.getElementById("signupRole").value = role;
        document
          .getElementById("chooseClient")
          .classList.toggle("active", isClient);
        document
          .getElementById("chooseVendor")
          .classList.toggle("active", !isClient);
        document.getElementById("clientTierGroup").style.display = isClient
          ? "block"
          : "none";
        document.getElementById("vendorSpecGroup").style.display = isClient
          ? "none"
          : "block";
        document.getElementById("nameLabel").textContent = isClient
          ? "Client Full Name"
          : "Vendor Organization Name";
        document.getElementById("fullName").placeholder = isClient
          ? "e.g., Maharaja Vikramaditya Rathore"
          : "e.g., Garuda Sovereign Security";
        document.getElementById("signupSubmitBtn").textContent = isClient
          ? "Create Client Account & Open Client Portal"
          : "Create Vendor Account & Open Vendor Portal";
      }

      function validateSignup() {
        if (
          document.getElementById("signupPass").value !==
          document.getElementById("signupConfirm").value
        ) {
          alert("Passcode and Confirm Passcode do not match!");
          return false;
        }
        return true;
      }

      function validateReset() {
        if (
          document.getElementById("newPass").value !==
          document.getElementById("confirmPass").value
        ) {
          alert("New Password and Confirm New Password do not match!");
          return false;
        }
        return true;
      }
    </script>
  </body>
</html>
