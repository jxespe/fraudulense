const navItems = document.querySelectorAll(".nav-item");
const pages = document.querySelectorAll(".page");
const activityBars = document.querySelectorAll("#activity-bars .bar");

const elements = {
  statTotalReviews: document.getElementById("stat-total-reviews"),
  statVerifiedValid: document.getElementById("stat-verified-valid"),
  statConfirmedFraud: document.getElementById("stat-confirmed-fraud"),
  statPendingReview: document.getElementById("stat-pending-review"),
  statTotalReviewsUsers: document.getElementById("stat-total-reviews-users"),
  statVerifiedValidUsers: document.getElementById("stat-verified-valid-users"),
  statConfirmedFraudUsers: document.getElementById("stat-confirmed-fraud-users"),
  statPendingReviewUsers: document.getElementById("stat-pending-review-users"),
  recentActivityTotal: document.getElementById("recent-activity-total"),
  recentActivityUnusual: document.getElementById("recent-activity-unusual"),
  recentFraudPercent: document.getElementById("recent-fraud-percent"),
  recentProfiles: document.getElementById("recent-profiles"),
  userList: document.getElementById("user-list"),
  userSearch: document.getElementById("user-search"),
  riskFilter: document.getElementById("risk-filter"),
  logUserName: document.getElementById("log-user-name"),
  logUserId: document.getElementById("log-user-id"),
  logEntries: document.getElementById("log-entries"),
  contentGrid: document.getElementById("content-grid"),
  sortDate: document.getElementById("sort-date"),
  sortRelevance: document.getElementById("sort-relevance"),
  contentSearch: document.getElementById("content-search"),
  premiumUsers: document.getElementById("stat-premium-users"),
  nonPremiumUsers: document.getElementById("stat-nonpremium-users"),
  premiumRatio: document.getElementById("stat-premium-ratio"),
  premiumRevenue: document.getElementById("stat-premium-revenue"),
  premium1m: document.getElementById("stat-premium-1m"),
  premium6m: document.getElementById("stat-premium-6m"),
  premium1y: document.getElementById("stat-premium-1y"),
  premiumTrial: document.getElementById("stat-premium-trial"),
  analyticsTotalUsers: document.getElementById("analytics-total-users"),
  analyticsChurnRisk: document.getElementById("analytics-churn-risk"),
  analyticsLtvTotal: document.getElementById("analytics-ltv-total"),
  analyticsLtvAvg: document.getElementById("analytics-ltv-avg"),
  analyticsTimeline: document.getElementById("analytics-timeline"),
  analyticsGaugeNeedle: document.getElementById("analytics-gauge-needle"),
  analyticsGaugeValue: document.getElementById("analytics-gauge-value"),
  analyticsAvgVisits: document.getElementById("analytics-avg-visits"),
  toggleAccountManagement: document.getElementById("toggle-account-management"),
  toggleContentModeration: document.getElementById("toggle-content-moderation"),
  toggleSystemNotification: document.getElementById("toggle-system-notification"),
  toggleDataReporting: document.getElementById("toggle-data-reporting"),
  billingBtn: document.getElementById("billing-btn"),
  premiumPanel: document.getElementById("premium-panel"),
  loginScreen: document.getElementById("login-screen"),
  adminShell: document.getElementById("admin-shell"),
  loginEmail: document.getElementById("login-email"),
  loginPassword: document.getElementById("login-password"),
  loginSubmit: document.getElementById("login-submit"),
  loginError: document.getElementById("login-error"),
  settingsBtn: document.getElementById("menu-btn"),
  settingsMenu: document.getElementById("settings-menu"),
  logoutBtn: document.getElementById("logout-btn"),
  reportsModal: document.getElementById("reports-modal"),
  reportsList: document.getElementById("reports-list"),
  closeReports: document.getElementById("close-reports"),
  postModal: document.getElementById("post-modal"),
  closePost: document.getElementById("close-post"),
  postImage: document.getElementById("post-image"),
  postUser: document.getElementById("post-user"),
  postDate: document.getElementById("post-date"),
  postCaption: document.getElementById("post-caption"),
  postShared: document.getElementById("post-shared"),
  sharedImage: document.getElementById("shared-image"),
  sharedUser: document.getElementById("shared-user"),
  sharedDate: document.getElementById("shared-date"),
  sharedCaption: document.getElementById("shared-caption")
};

const setActivePage = (targetId) => {
  pages.forEach((page) => {
    page.classList.toggle("active", page.id === targetId);
  });
  navItems.forEach((item) => {
    item.classList.toggle("active", item.dataset.target === targetId);
  });
};

const focusPremiumPanel = () => {
  setActivePage("billing");
  if (!elements.premiumPanel) return;
  elements.premiumPanel.scrollIntoView({ behavior: "smooth", block: "start" });
  elements.premiumPanel.classList.add("premium-highlight");
  window.setTimeout(() => {
    elements.premiumPanel.classList.remove("premium-highlight");
  }, 1600);
};

navItems.forEach((item) => {
  item.addEventListener("click", () => {
    setActivePage(item.dataset.target);
  });
});

setActivePage("dashboard");
if (elements.billingBtn) {
  elements.billingBtn.addEventListener("click", focusPremiumPanel);
}

const formatNumber = (value) =>
  new Intl.NumberFormat("en-US").format(value || 0);

const formatPercent = (value) => `${Math.round(value * 100)}%`;

const asDate = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate();
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000);
  return new Date(timestamp);
};

const getPostDate = (post) =>
  asDate(post.timestamp || post.createdAt || post.created_at);

const isFraudReport = (report) => {
  const result = String(report.result || "").toLowerCase();
  const label = report.label;
  return result.includes("scam") || result.includes("fraud") || label === 1;
};

const isValidReport = (report) => {
  const result = String(report.result || "").toLowerCase();
  const label = report.label;
  return result.includes("legit") || result.includes("safe") || label === 0;
};

const isPendingReport = (report) => {
  const status = String(report.status || "").toLowerCase();
  return status === "open" || status === "under review" || status === "pending";
};

const isPostUnusual = (post) => {
  const moderationStatus = String(post.moderationStatus || "").toLowerCase();
  return Boolean(
    post.isFraud === true ||
      moderationStatus === "fraud" ||
      moderationStatus === "flagged"
  );
};

const normalizePlanKey = (plan) => {
  if (!plan) return "";
  const key = String(plan).toLowerCase().trim();
  if (key.includes("1_month") || key.includes("1 month")) return "1_month";
  if (key.includes("6_month") || key.includes("6 month")) return "6_months";
  if (key.includes("1_year") || key.includes("1 year")) return "1_year";
  if (key.includes("trial") || key.includes("free")) return "trial";
  return key;
};

const getPremiumAmount = (user) => {
  const direct =
    user.premiumAmount ||
    user.premiumPrice ||
    user.planPrice ||
    user.amountPaid ||
    0;
  const numeric = Number(direct);
  if (!Number.isNaN(numeric) && numeric > 0) return numeric;
  const planKey = normalizePlanKey(user.premiumPlan || user.planType || user.plan);
  if (planKey === "1_month") return 349;
  if (planKey === "6_months") return 999;
  if (planKey === "1_year") return 1599;
  return 0;
};

const getPremiumTimestamp = (user) => {
  const candidates = [
    user.premiumStartedAt,
    user.premiumStart,
    user.premiumSince,
    user.premiumActivatedAt,
    user.premiumUpdatedAt,
    user.updatedAt,
    user.createdAt,
    user.timestamp
  ];
  for (const value of candidates) {
    const date = asDate(value);
    if (date) return date;
  }
  return null;
};

const renderActivityBars = (activityLogs) => {
  const now = Date.now();
  const hourBuckets = new Array(10).fill(0);
  activityLogs.forEach((log) => {
    const date = asDate(log.timestamp);
    if (!date) return;
    const diffHours = Math.floor((now - date.getTime()) / (1000 * 60 * 60));
    if (diffHours >= 0 && diffHours < 10) {
      hourBuckets[9 - diffHours] += 1;
    }
  });
  const max = Math.max(...hourBuckets, 1);
  hourBuckets.forEach((count, index) => {
    const height = Math.max(24, (count / max) * 170);
    if (activityBars[index]) {
      activityBars[index].style.height = `${height}px`;
    }
  });
};

const renderUserList = (users, selectedUserId, reportsByUser, onSelect) => {
  const query = (elements.userSearch.value || "").toLowerCase();
  const riskFilter = (elements.riskFilter.value || "").toLowerCase();

  const filtered = users.filter((user) => {
    const name = String(user.name || "").toLowerCase();
    const username = String(user.username || "").toLowerCase();
    const email = String(user.email || "").toLowerCase();
    const phone = String(user.phoneNumber || "").toLowerCase();
    const id = String(user.id || "").toLowerCase();
    const matchQuery =
      !query ||
      name.includes(query) ||
      username.includes(query) ||
      email.includes(query) ||
      phone.includes(query) ||
      id.includes(query);
    if (!matchQuery) return false;
    if (riskFilter === "low") return true;
    const reportCount = reportsByUser.get(user.id) || 0;
    if (riskFilter === "medium") return reportCount >= 2;
    if (riskFilter === "high") return reportCount >= 5;
    return true;
  });

  elements.userList.innerHTML = "";
  if (filtered.length === 0) {
    elements.userList.innerHTML = "<p class=\"empty-state\">No users found.</p>";
    return;
  }

  filtered.forEach((user) => {
    const row = document.createElement("div");
    row.className = `user-row ${user.id === selectedUserId ? "selected" : ""}`;
    row.dataset.userId = user.id;

    const reportCount = reportsByUser.get(user.id) || 0;
    const photoUrl = user.photoUrl || user.photoURL || user.profilePhotoUrl || "";
    row.innerHTML = `
      <div class="user-info">
        <div class="mini-avatar" style="${photoUrl ? `background-image:url('${photoUrl}')` : ""}"></div>
        <div>
          <strong>${user.name || user.username || "Anonymous"}</strong>
          <span>User ID: ${user.id}</span>
          <span>${user.email || ""}</span>
        </div>
      </div>
      <div class="actions">
        <button class="tag purple" data-action="review">Review Reports (${reportCount})</button>
        <button class="tag yellow" data-action="flag">Flag for Review</button>
        <button class="tag red" data-action="suspend">Suspend Account</button>
        <button class="tag red-outline" data-action="delete">Delete User</button>
      </div>
    `;
    row.addEventListener("click", (event) => {
      const actionBtn = event.target.closest("[data-action]");
      if (actionBtn) {
        handleUserAction(user, actionBtn.dataset.action);
        return;
      }
      if (event.target.closest(".actions")) return;
      onSelect(user);
    });
    elements.userList.appendChild(row);
  });
};

const renderActivityLog = (user, logs) => {
  if (!user) {
    elements.logUserName.textContent = "Select a user";
    elements.logUserId.textContent = "User ID: --";
    elements.logEntries.innerHTML = "<p>No activity yet.</p>";
    return;
  }
  elements.logUserName.textContent = user.name || user.username || "Anonymous";
  elements.logUserId.textContent = `User ID: ${user.id}`;

  if (logs.length === 0) {
    elements.logEntries.innerHTML = "<p>No activity yet.</p>";
    return;
  }

  elements.logEntries.innerHTML = "";
  logs.slice(0, 8).forEach((log) => {
    const date = asDate(log.timestamp);
    const line = document.createElement("p");
    const time = date
      ? date.toLocaleString("en-US", { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" })
      : "Unknown time";
    line.textContent = `${time} - ${log.action || "activity"}`;
    elements.logEntries.appendChild(line);
  });
};

const renderContents = (posts, usersById) => {
  const query = (elements.contentSearch?.value || "").toLowerCase();
  const filtered = posts.filter((post) => {
    if (!query) return true;
    const user = usersById.get(post.userId) || {};
    const name = String(post.userName || user.name || user.username || "").toLowerCase();
    const email = String(user.email || "").toLowerCase();
    return name.includes(query) || email.includes(query);
  });

  elements.contentGrid.innerHTML = "";
  if (filtered.length === 0) {
    elements.contentGrid.innerHTML = "<p class=\"empty-state\">No posts found.</p>";
    return;
  }

  filtered.forEach((post) => {
    const card = document.createElement("div");
    card.className = "content-card";
    const date = getPostDate(post);
    const userName = post.userName || usersById.get(post.userId)?.name || "Anonymous";
    const statusText = post.moderationStatus
      ? `Reviewed: ${post.moderationStatus}`
      : "Reviewed: Awaiting moderation.";

    card.innerHTML = `
      <strong>Posted by: ${userName}</strong>
      <span>Date: ${date ? date.toLocaleDateString("en-US") : "-"}</span>
      <button class="view-post-btn" type="button" data-action="view">View Post Details</button>
      <p>${statusText}</p>
      <p>${isPostUnusual(post) ? "Potential fraudulent content detected." : "No fraudulent content detected."}</p>
      <div class="card-actions">
        <button class="tag yellow" type="button" data-action="notify">Notify User</button>
        <button class="tag green" type="button" data-action="allow">Allow Post</button>
        <button class="tag red" type="button" data-action="reject">Reject Post</button>
      </div>
    `;
    card.querySelector("[data-action=\"notify\"]").addEventListener("click", (event) => {
      event.stopPropagation();
      if (!functions) return;
      const user = usersById.get(post.userId) || {};
      const postUserId = typeof post.userId === "string" ? post.userId : "";
      const emailFallback =
        user.email ||
        (postUserId.includes("@") ? postUserId : "");
      const callable = functions.httpsCallable("sendUserNotification");
      callable({
        userDocId: user.id || postUserId || "",
        email: emailFallback,
        title: "FrauduLens Warning",
        body: "Your post was flagged by the admin. Please review your content."
      })
        .then((result) => {
          const data = result && result.data ? result.data : {};
          if (data.ok) {
            updatePostModeration(post.id, "flagged");
            return;
          }
          const reason = data.reason || "unknown";
          window.alert(`Failed to notify user. Reason: ${reason}`);
        })
        .catch((error) => {
          console.error("Failed to send notification", error);
          const message =
            error && error.message
              ? error.message
              : "Failed to notify user. Check FCM tokens.";
          window.alert(message);
        });
    });
    card.querySelector("[data-action=\"allow\"]").addEventListener("click", (event) => {
      event.stopPropagation();
      updatePostModeration(post.id, "allowed");
    });
    card.querySelector("[data-action=\"reject\"]").addEventListener("click", (event) => {
      event.stopPropagation();
      if (!post.id) return;
      const confirmed = window.confirm("Reject and delete this post?");
      if (!confirmed) return;
      db.collection("posts")
        .doc(post.id)
        .delete()
        .catch((error) => console.error("Failed to delete post", error));
    });
    card.querySelector("[data-action=\"view\"]").addEventListener("click", (event) => {
      event.stopPropagation();
      openPostModal(post, usersById);
    });
    elements.contentGrid.appendChild(card);
  });
};

const updatePostModeration = (postId, status) => {
  if (!postId) return;
  db.collection("posts")
    .doc(postId)
    .update({
      moderationStatus: status,
      moderationUpdatedAt: firebase.firestore.FieldValue.serverTimestamp()
    })
    .catch((error) => console.error("Failed to update moderation status", error));
};

let db;
let auth;
let functions;
let cachedUsers = [];
let cachedPosts = [];
let cachedReports = [];
let cachedLogs = [];
let selectedUser = null;
let featureSettingsUnsub = null;

const initFirebase = () => {
  if (!window.FIREBASE_CONFIG || FIREBASE_CONFIG.apiKey === "YOUR_API_KEY") {
    console.warn("Firebase config missing. Update firebase-config.js.");
    return null;
  }
  firebase.initializeApp(FIREBASE_CONFIG);
  auth = firebase.auth();
  functions = firebase.app().functions("asia-southeast1");
  return firebase.firestore();
};

const updateDashboardStats = () => {
  const totalReviews = cachedReports.length;
  const fraudCount = cachedReports.filter(isFraudReport).length;
  const validCount = cachedReports.filter(isValidReport).length;
  const pendingCount = cachedReports.filter(isPendingReport).length;

  elements.statTotalReviews.textContent = formatNumber(totalReviews);
  elements.statVerifiedValid.textContent = formatNumber(validCount);
  elements.statConfirmedFraud.textContent = formatNumber(fraudCount);
  elements.statPendingReview.textContent = formatNumber(pendingCount);

  elements.statTotalReviewsUsers.textContent = formatNumber(totalReviews);
  elements.statVerifiedValidUsers.textContent = formatNumber(validCount);
  elements.statConfirmedFraudUsers.textContent = formatNumber(fraudCount);
  elements.statPendingReviewUsers.textContent = formatNumber(pendingCount);

  const recentLogs = cachedLogs.filter((log) => {
    const date = asDate(log.timestamp);
    return date && Date.now() - date.getTime() <= 24 * 60 * 60 * 1000;
  });
  const unusualCount = cachedPosts.filter(isPostUnusual).length + fraudCount;

  elements.recentActivityTotal.textContent = formatNumber(recentLogs.length);
  elements.recentActivityUnusual.textContent = formatNumber(unusualCount);
  elements.recentFraudPercent.textContent = formatPercent(
    totalReviews === 0 ? 0 : fraudCount / totalReviews
  );
  elements.recentProfiles.textContent = formatNumber(cachedUsers.length);

  const premiumUsers = cachedUsers.filter((user) => {
    return (
      user.isPremium === true ||
      String(user.premiumStatus || "").toLowerCase() === "active" ||
      Boolean(user.premiumPlan || user.planType || user.plan)
    );
  });
  const premiumCount = premiumUsers.length;
  const nonPremiumCount = Math.max(0, cachedUsers.length - premiumCount);
  const ratio = cachedUsers.length === 0 ? 0 : premiumCount / cachedUsers.length;
  const planBuckets = {
    "1_month": 0,
    "6_months": 0,
    "1_year": 0,
    trial: 0
  };
  let totalRevenue = 0;
  premiumUsers.forEach((user) => {
    const planKey = normalizePlanKey(user.premiumPlan || user.planType || user.plan);
    if (planKey && planBuckets[planKey] !== undefined) {
      planBuckets[planKey] += 1;
    } else if (user.isPremium === true && !planKey) {
      planBuckets.trial += 1;
    }
    totalRevenue += getPremiumAmount(user);
  });

  if (elements.premiumUsers) elements.premiumUsers.textContent = formatNumber(premiumCount);
  if (elements.nonPremiumUsers) elements.nonPremiumUsers.textContent = formatNumber(nonPremiumCount);
  if (elements.premiumRatio) elements.premiumRatio.textContent = formatPercent(ratio);
  if (elements.premiumRevenue) elements.premiumRevenue.textContent = `₱${formatNumber(totalRevenue)}`;
  if (elements.premium1m) elements.premium1m.textContent = formatNumber(planBuckets["1_month"]);
  if (elements.premium6m) elements.premium6m.textContent = formatNumber(planBuckets["6_months"]);
  if (elements.premium1y) elements.premium1y.textContent = formatNumber(planBuckets["1_year"]);
  if (elements.premiumTrial) elements.premiumTrial.textContent = formatNumber(planBuckets.trial);

  const totalUsers = cachedUsers.length;
  if (elements.analyticsTotalUsers) {
    elements.analyticsTotalUsers.textContent = formatNumber(totalUsers);
  }
  if (elements.analyticsChurnRisk) {
    const churnRisk = totalUsers === 0 ? 0 : nonPremiumCount / totalUsers;
    elements.analyticsChurnRisk.textContent = formatPercent(churnRisk);
  }
  if (elements.analyticsLtvTotal) {
    elements.analyticsLtvTotal.textContent = `₱${formatNumber(totalRevenue)}`;
  }
  if (elements.analyticsLtvAvg) {
    const avgLtv = premiumCount === 0 ? 0 : totalRevenue / premiumCount;
    elements.analyticsLtvAvg.textContent = `₱${formatNumber(Math.round(avgLtv))}`;
  }
  if (elements.analyticsGaugeNeedle) {
    const angle = -60 + ratio * 120;
    elements.analyticsGaugeNeedle.style.transform = `rotate(${angle}deg)`;
  }
  if (elements.analyticsGaugeValue) {
    elements.analyticsGaugeValue.textContent = formatPercent(ratio);
  }
  if (elements.analyticsAvgVisits) {
    const avgVisits = totalUsers === 0 ? 0 : cachedLogs.length / totalUsers;
    elements.analyticsAvgVisits.textContent = avgVisits.toFixed(1);
  }

  const planBars = document.querySelectorAll("[data-plan-bar]");
  const maxPlan = Math.max(...Object.values(planBuckets), 1);
  planBars.forEach((bar) => {
    const key = bar.getAttribute("data-plan-bar");
    const value = planBuckets[key] || 0;
    const width = (value / maxPlan) * 100;
    bar.style.width = `${Math.max(8, width)}%`;
  });

  const planCols = document.querySelectorAll("[data-plan-col]");
  planCols.forEach((col) => {
    const key = col.getAttribute("data-plan-col");
    const value = planBuckets[key] || 0;
    const height = (value / maxPlan) * 100;
    col.style.height = `${Math.max(20, height)}%`;
  });

  if (elements.analyticsTimeline) {
    const recentPremium = premiumUsers
      .map((user) => ({ user, date: getPremiumTimestamp(user) }))
      .filter((item) => item.date)
      .sort((a, b) => b.date - a.date)
      .slice(0, 3);
    elements.analyticsTimeline.innerHTML = "";
    if (recentPremium.length === 0) {
      elements.analyticsTimeline.innerHTML = "<p class=\"empty-state\">No recent premium activity.</p>";
    } else {
      recentPremium.forEach(({ user, date }) => {
        const item = document.createElement("div");
        item.className = "timeline-item";
        const name = user.name || user.username || user.email || "Premium user";
        const plan = normalizePlanKey(user.premiumPlan || user.planType || user.plan) || "premium";
        item.innerHTML = `
          <div class="dot"></div>
          <div>
            <strong>${name}</strong>
            <span>${plan.replace("_", " ")} • ${date.toLocaleString("en-US")}</span>
          </div>
        `;
        elements.analyticsTimeline.appendChild(item);
      });
    }
  }

  renderActivityBars(recentLogs);
};

const buildReportsByUser = () => {
  const map = new Map();
  cachedReports.forEach((report) => {
    if (!report.userId) return;
    map.set(report.userId, (map.get(report.userId) || 0) + 1);
  });
  return map;
};

const refreshUserPanel = () => {
  const reportsByUser = buildReportsByUser();
  const handleSelect = (user) => {
    selectedUser = user;
    const userLogs = cachedLogs.filter((log) => log.user === user.email);
    renderActivityLog(user, userLogs);
    renderUserList(cachedUsers, selectedUser?.id, reportsByUser, handleSelect);
  };
  renderUserList(cachedUsers, selectedUser?.id, reportsByUser, handleSelect);
};

const sortPostsByDate = () => {
  cachedPosts.sort((a, b) => {
    const ta = getPostDate(a)?.getTime() || 0;
    const tb = getPostDate(b)?.getTime() || 0;
    return tb - ta;
  });
};

const sortPostsByRelevance = () => {
  cachedPosts.sort((a, b) => {
    const scoreA = (a.commentCount || 0) + (a.likeCount || 0) + (a.shareCount || 0);
    const scoreB = (b.commentCount || 0) + (b.likeCount || 0) + (b.shareCount || 0);
    return scoreB - scoreA;
  });
};

const refreshContentGrid = () => {
  const usersById = new Map(cachedUsers.map((user) => [user.id, user]));
  renderContents(cachedPosts, usersById);
};

const attachFilters = () => {
  elements.userSearch.addEventListener("input", refreshUserPanel);
  elements.riskFilter.addEventListener("change", refreshUserPanel);
  elements.sortDate.addEventListener("click", () => {
    sortPostsByDate();
    refreshContentGrid();
  });
  elements.sortRelevance.addEventListener("click", () => {
    sortPostsByRelevance();
    refreshContentGrid();
  });
  if (elements.contentSearch) {
    elements.contentSearch.addEventListener("input", refreshContentGrid);
  }
  elements.closeReports.addEventListener("click", () => {
    elements.reportsModal.classList.add("hidden");
  });
  elements.reportsModal.addEventListener("click", (event) => {
    if (event.target === elements.reportsModal) {
      elements.reportsModal.classList.add("hidden");
    }
  });
  if (elements.closePost) {
    elements.closePost.addEventListener("click", () => {
      elements.postModal.classList.add("hidden");
    });
  }
  if (elements.postModal) {
    elements.postModal.addEventListener("click", (event) => {
      if (event.target === elements.postModal) {
        elements.postModal.classList.add("hidden");
      }
    });
  }
};

const setImagePreview = (el, url) => {
  if (!el) return;
  if (url) {
    el.style.backgroundImage = `url('${url}')`;
  } else {
    el.style.backgroundImage = "none";
  }
};

const openPostModal = (post, usersById) => {
  if (!elements.postModal) return;
  const user = usersById.get(post.userId) || {};
  const userName = post.userName || user.name || user.username || "Anonymous";
  const date = getPostDate(post);
  const caption = post.caption || "";
  const imageUrl = post.imageUrl || "";
  elements.postUser.textContent = userName;
  elements.postDate.textContent = date ? date.toLocaleString("en-US") : "-";
  elements.postCaption.textContent = caption || "No caption";
  setImagePreview(elements.postImage, imageUrl);

  const shared = post.sharedPost || null;
  if (shared) {
    elements.postShared.style.display = "grid";
    elements.sharedUser.textContent = shared.userName || "Unknown";
    const sharedDate = asDate(shared.timestamp);
    elements.sharedDate.textContent = sharedDate ? sharedDate.toLocaleString("en-US") : "-";
    elements.sharedCaption.textContent = shared.caption || "No caption";
    setImagePreview(elements.sharedImage, shared.imageUrl || "");
  } else {
    elements.postShared.style.display = "none";
  }

  elements.postModal.classList.remove("hidden");
};

const bindFeatureToggles = () => {
  const toggles = [
    { key: "accountManagement", el: elements.toggleAccountManagement },
    { key: "contentModeration", el: elements.toggleContentModeration },
    { key: "systemNotification", el: elements.toggleSystemNotification },
    { key: "dataReporting", el: elements.toggleDataReporting }
  ];
  toggles.forEach((item) => {
    if (!item.el) return;
    item.el.addEventListener("change", (event) => {
      if (!db) return;
      db.collection("admin_settings")
        .doc("features")
        .set({ [item.key]: event.target.checked }, { merge: true })
        .catch((error) => console.error("Failed to update feature toggle", error));
    });
  });

  if (!db) return;
  if (featureSettingsUnsub) {
    featureSettingsUnsub();
  }
  featureSettingsUnsub = db
    .collection("admin_settings")
    .doc("features")
    .onSnapshot((doc) => {
      const data = doc.exists ? doc.data() : {};
      toggles.forEach((item) => {
        if (!item.el) return;
        if (typeof data[item.key] === "boolean") {
          item.el.checked = data[item.key];
        }
      });
    });
};

const formatReportTime = (report) => {
  const date = asDate(report.timestamp);
  return date ? date.toLocaleString("en-US") : "Unknown time";
};

const renderReportsModal = (user) => {
  const email = user.email || "";
  const reports = cachedReports.filter((report) => {
    return report.userId === user.id || report.userId === email;
  });
  elements.reportsList.innerHTML = "";
  if (reports.length === 0) {
    elements.reportsList.innerHTML = "<p class=\"empty-state\">No reports yet.</p>";
  } else {
    reports.forEach((report) => {
      const row = document.createElement("div");
      row.className = "modal-row";
      row.innerHTML = `
        <strong>${report.result || "Reported Scam"}</strong>
        <span>${formatReportTime(report)}</span>
        <span>${report.message || report.source || ""}</span>
      `;
      elements.reportsList.appendChild(row);
    });
  }
  elements.reportsModal.classList.remove("hidden");
};

const handleUserAction = (user, action) => {
  if (!db || !user) return;
  if (action === "delete") {
    const confirmed = window.confirm(`Delete ${user.name || user.email || "this user"}? This cannot be undone.`);
    if (!confirmed) return;
    if (!functions) return;
    const callable = functions.httpsCallable("deleteUserAccount");
    callable({
      userDocId: user.id,
      authUid: user.authUid || user.uid || "",
      email: user.email || ""
    })
      .then(() => {
        window.alert("User deleted.");
      })
      .catch((error) => {
        console.error("Failed to delete user", error);
        window.alert("Failed to delete user. Check admin permissions.");
      });
    return;
  }
  if (action === "suspend") {
    const durationInput = window.prompt("Suspend for how many days? (default 7)", "7");
    if (durationInput === null) return;
    const days = Math.max(1, parseInt(durationInput, 10) || 7);
    const reason = window.prompt("Reason for suspension (optional)", "");
    const now = new Date();
    const until = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    db.collection("users")
      .doc(user.id)
      .update({
        suspended: true,
        suspendedAt: firebase.firestore.Timestamp.fromDate(now),
        suspendedUntil: firebase.firestore.Timestamp.fromDate(until),
        suspendedReason: reason || "",
        suspendedBy: auth?.currentUser?.email || ""
      })
      .catch((error) => console.error("Failed to suspend user", error));
    return;
  }
  if (action === "flag") {
    const note = window.prompt("Add a note for review", "");
    if (note === null || note.trim() === "") return;
    db.collection("user_flags")
      .add({
        userId: user.id,
        email: user.email || "",
        note: note.trim(),
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
        createdBy: auth?.currentUser?.email || ""
      })
      .catch((error) => console.error("Failed to flag user", error));
    db.collection("users")
      .doc(user.id)
      .update({
        flagged: true,
        flagNote: note.trim(),
        flaggedAt: firebase.firestore.FieldValue.serverTimestamp()
      })
      .catch(() => {});
    return;
  }
  if (action === "review") {
    renderReportsModal(user);
  }
};

const bindSettingsMenu = () => {
  elements.settingsBtn.addEventListener("click", () => {
    elements.settingsMenu.classList.toggle("show");
  });
  document.addEventListener("click", (event) => {
    if (
      !elements.settingsMenu.contains(event.target) &&
      !elements.settingsBtn.contains(event.target)
    ) {
      elements.settingsMenu.classList.remove("show");
    }
  });
  elements.logoutBtn.addEventListener("click", () => {
    if (auth) {
      auth.signOut();
    }
  });
};

const showAdminUI = () => {
  elements.loginScreen.classList.add("hidden");
  elements.adminShell.classList.remove("hidden");
  attachFilters();
  bindSettingsMenu();
  bindFeatureToggles();
  listenData();
};

const showLoginUI = () => {
  elements.adminShell.classList.add("hidden");
  elements.loginScreen.classList.remove("hidden");
};

const bindAuth = () => {
  const attemptLogin = () => {
    const email = (elements.loginEmail.value || "").trim();
    const password = (elements.loginPassword.value || "").trim();
    if (!email || !password) {
      elements.loginError.textContent = "Enter email and password.";
      return;
    }
    elements.loginError.textContent = "";
    const doSignIn = () => {
      auth
        .signInWithEmailAndPassword(email, password)
        .then((cred) => {
          showAdminUI();
          loadAdminProfile(cred.user);
        })
        .catch((error) => {
          elements.loginError.textContent = error.message || "Login failed.";
        });
    };
    if (auth && auth.setPersistence) {
      auth
        .setPersistence(firebase.auth.Auth.Persistence.LOCAL)
        .then(doSignIn)
        .catch(doSignIn);
    } else {
      doSignIn();
    }
  };

  elements.loginSubmit.addEventListener("click", attemptLogin);
  elements.loginPassword.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      attemptLogin();
    }
  });

  auth.onAuthStateChanged((user) => {
    if (user) {
      showAdminUI();
      loadAdminProfile(user);
    } else {
      showLoginUI();
      elements.adminName && (elements.adminName.textContent = "Admin");
    }
  });
};

const loadAdminProfile = (user) => {
  const fallbackName = user.email ? user.email.split("@")[0] : "Admin";
  const nameEl = document.getElementById("admin-name");
  if (!user || !db) {
    if (nameEl) nameEl.textContent = fallbackName;
    return;
  }
  db.collection("admin_users")
    .doc(user.uid)
    .get()
    .then((doc) => {
      if (doc.exists) {
        const data = doc.data() || {};
        if (nameEl) nameEl.textContent = data.name || data.email || fallbackName;
      } else if (nameEl) {
        nameEl.textContent = fallbackName;
      }
    })
    .catch(() => {
      if (nameEl) nameEl.textContent = fallbackName;
    });
};
const listenData = () => {
  db.collection("users")
    .onSnapshot((snap) => {
      cachedUsers = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
      updateDashboardStats();
      refreshUserPanel();
      refreshContentGrid();
    });

  db.collection("posts")
    .onSnapshot((snap) => {
      cachedPosts = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
      sortPostsByDate();
      updateDashboardStats();
      refreshContentGrid();
    });

  db.collection("reports")
    .onSnapshot((snap) => {
      cachedReports = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
      updateDashboardStats();
      refreshUserPanel();
    });

  db.collection("activity_logs")
    .orderBy("timestamp", "desc")
    .limit(200)
    .onSnapshot((snap) => {
      cachedLogs = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
      updateDashboardStats();
      if (selectedUser) {
        const userLogs = cachedLogs.filter((log) => log.user === selectedUser.email);
        renderActivityLog(selectedUser, userLogs);
      }
    });
};

db = initFirebase();
if (db) {
  bindAuth();
} else {
  console.warn("Firebase not initialized. Admin data will not load.");
}
