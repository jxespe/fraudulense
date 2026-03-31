<?php
// Cloud Run/PHP buildpacks route requests through index.php.
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

if ($path === "/admin-register.php" || $path === "/admin-register") {
    require __DIR__ . "/admin-register.php";
    return;
}

require __DIR__ . "/admin.php";
