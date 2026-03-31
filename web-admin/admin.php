<?php
// Static admin UI served via PHP for easy hosting.
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>FrauduLenseAdmin</title>
    <?php
      function inline_asset($path) {
        $full = __DIR__ . "/" . ltrim($path, "/");
        if (file_exists($full)) {
          echo file_get_contents($full);
        }
      }
    ?>
    <style>
      <?php inline_asset("styles.css"); ?>
    </style>
  </head>
  <body>
    <div class="login-screen" id="login-screen">
      <div class="login-card">
        <div class="login-avatar"></div>
        <h2>ADMIN</h2>
        <label class="login-field">
          <span class="field-icon">✉</span>
          <input type="email" id="login-email" placeholder="Username" />
        </label>
        <label class="login-field">
          <span class="field-icon">🔒</span>
          <input type="password" id="login-password" placeholder="Password" />
          <span class="field-icon right">👁</span>
        </label>
        <div class="login-row">
          <label class="remember">
            <input type="checkbox" checked />
            Remember me
          </label>
          <button class="link-btn" type="button">Forgot Password?</button>
        </div>
        <button class="solid-btn login-btn" id="login-submit">Login</button>
        <p class="login-error" id="login-error"></p>
      </div>
      <p class="login-tagline">Clear vision, Secure decisions</p>
    </div>

    <div class="app-shell hidden" id="admin-shell">
      <aside class="sidebar">
        <div class="brand-mark" id="menu-btn">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <nav class="nav">
          <button class="nav-item active" data-target="dashboard">
            <span class="nav-icon" data-icon="dashboard"></span>
            <span class="nav-label">Dashboard</span>
          </button>
          <button class="nav-item" data-target="features">
            <span class="nav-icon" data-icon="features"></span>
            <span class="nav-label">Feature Management</span>
          </button>
          <button class="nav-item" data-target="users">
            <span class="nav-icon" data-icon="users"></span>
            <span class="nav-label">User Management</span>
          </button>
          <button class="nav-item" data-target="contents">
            <span class="nav-icon" data-icon="contents"></span>
            <span class="nav-label">Contents</span>
          </button>
          <div class="nav-divider" aria-hidden="true"></div>
          <button class="icon-btn" id="billing-btn" aria-label="Billing">
            <span class="nav-icon" data-icon="billing"></span>
          </button>
          <button class="icon-btn" id="settings-btn" aria-label="Settings">
            <span class="nav-icon" data-icon="settings"></span>
          </button>
        </nav>
      </aside>

      <main class="main">
        <header class="topbar">
          <div class="logo">
            <div class="lens-dot"></div>
            <span>FrauduLenseAdmin</span>
            <span class="divider">|</span>
            <span class="role">Admin</span>
          </div>
          <div class="search">
            <input type="text" placeholder="Search" />
            <span class="search-icon">🔍</span>
          </div>
          <div class="settings-menu" id="settings-menu">
            <button id="logout-btn">Logout</button>
          </div>
        </header>

        <section class="profile">
          <div class="avatar">
            <div class="avatar-inner"></div>
          </div>
          <div>
            <h1 id="admin-name">Randy Vuitton</h1>
            <div class="divider-line"></div>
          </div>
        </section>

        <section id="dashboard" class="page active">
          <h2>Dashboard</h2>
          <div class="stat-grid">
            <div class="stat-card">
              <span>Total Activity Review</span>
              <strong id="stat-total-reviews">0</strong>
            </div>
            <div class="stat-card">
              <span>Verified as Valid</span>
              <strong id="stat-verified-valid">0</strong>
            </div>
            <div class="stat-card">
              <span>Confirmed as Fraud</span>
              <strong id="stat-confirmed-fraud">0</strong>
            </div>
            <div class="stat-card">
              <span>Pending Review</span>
              <strong id="stat-pending-review">0</strong>
            </div>
          </div>

          <div class="dashboard-grid">
            <div class="panel chart-panel">
              <h3>Activity By Hour</h3>
              <div class="chart">
                <div class="bar-set" id="activity-bars">
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                  <div class="bar"></div>
                </div>
                <button class="ghost-btn">Export Screenshot</button>
              </div>
            </div>
            <div class="panel activity-panel">
              <div class="activity-card">
                <span>Recent Activity</span>
                <strong id="recent-activity-total">0</strong>
              </div>
              <div class="activity-card">
                <span>Unusual</span>
                <strong id="recent-activity-unusual">0</strong>
              </div>
              <div class="activity-card">
                <span>Fraud Detected</span>
                <strong id="recent-fraud-percent">0%</strong>
              </div>
              <div class="activity-card">
                <span>Profile Identified</span>
                <strong id="recent-profiles">0</strong>
              </div>
            </div>
          </div>

        </section>

        <section id="billing" class="page">
          <h2>Analytics Page</h2>
          <div class="analytics-grid" id="premium-panel">
            <div class="panel analytics-card profile-card">
              <div class="profile-header">
                <div class="profile-photo"></div>
                <div class="profile-meta">
                  <strong>FrauduLens Premium</strong>
                  <span>Live subscription overview</span>
                </div>
              </div>
              <div class="profile-stats">
                <div class="pill"><span id="stat-premium-users">0</span> Premium</div>
                <div class="pill"><span id="stat-nonpremium-users">0</span> Non-Premium</div>
              </div>
              <div class="pill-row">
                <span>Total Users: <strong id="analytics-total-users">0</strong></span>
                <span>Premium Ratio: <strong id="stat-premium-ratio">0%</strong></span>
              </div>
              <div class="pill-group">
                <span class="pill soft">Revenue: <strong id="stat-premium-revenue">₱0</strong></span>
                <span class="pill soft">1 Month: <strong id="stat-premium-1m">0</strong></span>
                <span class="pill soft">6 Months: <strong id="stat-premium-6m">0</strong></span>
                <span class="pill soft">1 Year: <strong id="stat-premium-1y">0</strong></span>
              </div>
              <div class="mini-section">
                <h4>Insights</h4>
                <ul>
                  <li>Free trials: <strong id="stat-premium-trial">0</strong></li>
                  <li>Average revenue per premium: <strong id="analytics-ltv-avg">₱0</strong></li>
                  <li>Estimated churn risk: <strong id="analytics-churn-risk">0%</strong></li>
                </ul>
              </div>
            </div>

            <div class="panel analytics-card">
              <h4>Plans Breakdown</h4>
              <div class="bar-list">
                <div><span>1 Month</span><span class="bar" data-plan-bar="1_month"></span></div>
                <div><span>6 Months</span><span class="bar" data-plan-bar="6_months"></span></div>
                <div><span>1 Year</span><span class="bar" data-plan-bar="1_year"></span></div>
                <div><span>Trial</span><span class="bar" data-plan-bar="trial"></span></div>
              </div>
            </div>

            <div class="panel analytics-card timeline-card">
              <h4>Recent Premium Activity</h4>
              <div class="timeline" id="analytics-timeline"></div>
            </div>

            <div class="panel analytics-card">
              <h4>Churn Risk</h4>
              <div class="sparkline"></div>
              <p class="mini-note">Non-premium ratio drives churn risk.</p>
            </div>

            <div class="panel analytics-card">
              <h4>Customer lifetime value</h4>
              <div class="bar-chart">
                <div class="bar-col" data-plan-col="1_month"></div>
                <div class="bar-col" data-plan-col="6_months"></div>
                <div class="bar-col" data-plan-col="1_year"></div>
                <div class="bar-col" data-plan-col="trial"></div>
              </div>
              <p class="mini-note">Total revenue: <strong id="analytics-ltv-total">₱0</strong></p>
            </div>

            <div class="panel analytics-card gauge-card">
              <h4>Engagement score</h4>
              <div class="gauge">
                <div class="needle" id="analytics-gauge-needle"></div>
                <div class="gauge-value" id="analytics-gauge-value">0%</div>
              </div>
            </div>

            <div class="panel analytics-card">
              <h4>Average visits</h4>
              <div class="sparkline"></div>
              <p class="mini-note">Avg activity per user: <strong id="analytics-avg-visits">0</strong></p>
            </div>
          </div>
        </section>

        <section id="features" class="page">
          <h2>Features Management</h2>
          <div class="feature-card">
            <div class="row">
              <span>Post Automatic Approval</span>
              <label class="toggle">
                <input type="checkbox" checked />
                <span class="slider"></span>
              </label>
            </div>
            <div class="row">
              <span>Modify Application Layout</span>
              <button class="arrow-btn">›</button>
            </div>
            <div class="stack">
              <strong>Access Control</strong>
              <div class="toggle-row">
                <span>Account Management</span>
                <label class="toggle">
                  <input type="checkbox" id="toggle-account-management" />
                  <span class="slider"></span>
                </label>
              </div>
              <div class="toggle-row">
                <span>Content Moderation</span>
                <label class="toggle">
                  <input type="checkbox" id="toggle-content-moderation" />
                  <span class="slider"></span>
                </label>
              </div>
              <div class="toggle-row">
                <span>System Notification</span>
                <label class="toggle">
                  <input type="checkbox" id="toggle-system-notification" />
                  <span class="slider"></span>
                </label>
              </div>
              <div class="toggle-row">
                <span>Data & Reporting Access</span>
                <label class="toggle">
                  <input type="checkbox" id="toggle-data-reporting" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>
            <div class="row">
              <span>Security Settings</span>
              <button class="arrow-btn">›</button>
            </div>
            <div class="row">
              <span>Data Backup and Restore</span>
              <button class="arrow-btn">›</button>
            </div>
            <div class="row">
              <span>Analytics and Reporting</span>
              <button class="arrow-btn">›</button>
            </div>
            <div class="action-row">
              <button class="ghost-btn">Preview</button>
              <button class="solid-btn">Save Changes</button>
            </div>
          </div>
        </section>

        <section id="users" class="page">
          <h2>User Management</h2>
          <div class="stat-grid">
            <div class="stat-card">
              <span>Total Activity Review</span>
              <strong id="stat-total-reviews-users">0</strong>
            </div>
            <div class="stat-card">
              <span>Verified as Valid</span>
              <strong id="stat-verified-valid-users">0</strong>
            </div>
            <div class="stat-card">
              <span>Confirmed as Fraud</span>
              <strong id="stat-confirmed-fraud-users">0</strong>
            </div>
            <div class="stat-card">
              <span>Pending Review</span>
              <strong id="stat-pending-review-users">0</strong>
            </div>
          </div>

          <div class="user-grid">
            <div class="panel flagged-panel">
              <div class="panel-head">
                <div class="input-chip">
                  <span class="chip-icon">🔍</span>
                  <input type="text" placeholder="Search user" id="user-search" />
                </div>
                <div class="risk-select">
                  <span>Risk Level</span>
                  <select id="risk-filter">
                    <option>Low</option>
                    <option>Medium</option>
                    <option>High</option>
                  </select>
                </div>
              </div>
              <h3>Flagged Content Feed</h3>
              <div id="user-list"></div>
            </div>
            <div class="panel log-panel">
              <h3>Activity Log</h3>
              <div class="log-card">
                <div class="log-user">
                  <div class="mini-avatar"></div>
                  <div>
                    <strong id="log-user-name">Select a user</strong>
                    <span id="log-user-id">User ID: --</span>
                  </div>
                </div>
                <div class="log-entries" id="log-entries">
                  <p>No activity yet.</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <div class="modal hidden" id="reports-modal">
          <div class="modal-card">
            <div class="modal-head">
              <h3>Reported Activity</h3>
              <button class="icon-btn small" id="close-reports">✕</button>
            </div>
            <div class="modal-body" id="reports-list">
              <p class="empty-state">No reports yet.</p>
            </div>
          </div>
        </div>

        <div class="modal hidden" id="post-modal">
          <div class="modal-card post-modal-card">
            <div class="modal-head">
              <h3>Post Details</h3>
              <button class="icon-btn small" id="close-post">✕</button>
            </div>
            <div class="modal-body">
              <div class="post-preview">
                <div class="post-image" id="post-image"></div>
                <div class="post-meta">
                  <strong id="post-user"></strong>
                  <span id="post-date"></span>
                </div>
                <p id="post-caption"></p>
              </div>
              <div class="post-shared" id="post-shared">
                <strong>Shared Post</strong>
                <div class="post-preview compact">
                  <div class="post-image" id="shared-image"></div>
                  <div class="post-meta">
                    <strong id="shared-user"></strong>
                    <span id="shared-date"></span>
                  </div>
                  <p id="shared-caption"></p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <section id="contents" class="page">
          <h2>Contents</h2>
          <div class="sort-row">
            <span>Sort by:</span>
            <button class="ghost-btn" id="sort-date">Date</button>
            <button class="ghost-btn" id="sort-relevance">Relevance</button>
            <div class="input-chip content-search">
              <span class="chip-icon">🔍</span>
              <input type="text" placeholder="Search username" id="content-search" />
            </div>
          </div>
          <div class="content-grid" id="content-grid"></div>
        </section>
      </main>
    </div>

    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-firestore-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-auth-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-functions-compat.js"></script>
    <script>
      <?php inline_asset("firebase-config.js"); ?>
    </script>
    <script>
      <?php inline_asset("app.js"); ?>
    </script>
  </body>
</html>
