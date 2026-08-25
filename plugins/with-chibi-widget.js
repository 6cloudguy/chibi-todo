const { withAndroidManifest, withDangerousMod, createRunOncePlugin } = require("@expo/config-plugins");
const fs = require("fs");
const path = require("path");

const PLUGIN_NAME = "with-chibi-widget";

function ensureComponent(components, component, name) {
  const exists = components.some((entry) => entry.$?.["android:name"] === name);
  if (!exists) components.push(component);
}

function withWidgetManifest(config) {
  return withAndroidManifest(config, (manifestConfig) => {
    const application = manifestConfig.modResults.manifest.application?.[0];
    if (!application) throw new Error("Android application manifest entry was not found.");
    const androidPackage = config.android?.package;
    if (!androidPackage) throw new Error("An Android package is required to register the chibi widget receiver.");
    const receiverName = `${androidPackage}.ChibiWidgetProvider`;

    application.receiver = application.receiver ?? [];
    ensureComponent(application.receiver, {
      $: {
        "android:name": receiverName,
        "android:label": "Momo Companion",
        "android:exported": "true",
      },
      "intent-filter": [{
        action: [
          { $: { "android:name": "android.appwidget.action.APPWIDGET_UPDATE" } },
          { $: { "android:name": "android.appwidget.action.APPWIDGET_OPTIONS_CHANGED" } },
        ],
      }],
      "meta-data": [{
        $: {
          "android:name": "android.appwidget.provider",
          "android:resource": "@xml/chibi_widget_info",
        },
      }],
    }, receiverName);

    application.service = application.service ?? [];
    ensureComponent(application.service, {
      $: {
        "android:name": `${androidPackage}.MomoOverlayService`,
        "android:exported": "false",
        "android:foregroundServiceType": "specialUse",
      },
      property: [{
        $: {
          "android:name": "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE",
          "android:value": "Displays a user-requested, draggable Momo companion overlay.",
        },
      }],
    }, `${androidPackage}.MomoOverlayService`);

    const permissions = manifestConfig.modResults.manifest["uses-permission"] ?? [];
    ["android.permission.SYSTEM_ALERT_WINDOW", "android.permission.FOREGROUND_SERVICE_SPECIAL_USE", "android.permission.PACKAGE_USAGE_STATS"].forEach((permission) => {
      if (!permissions.some((item) => item.$?.["android:name"] === permission)) permissions.push({ $: { "android:name": permission } });
    });
    manifestConfig.modResults.manifest["uses-permission"] = permissions;

    return manifestConfig;
  });
}

function writeTemplate(sourcePath, destinationPath, androidPackage) {
  const template = fs.readFileSync(sourcePath, "utf8");
  fs.mkdirSync(path.dirname(destinationPath), { recursive: true });
  fs.writeFileSync(destinationPath, template.replaceAll("__CHIBI_WIDGET_PACKAGE__", androidPackage));
}

function copyDirectory(sourcePath, destinationPath) {
  fs.mkdirSync(destinationPath, { recursive: true });
  fs.cpSync(sourcePath, destinationPath, { recursive: true, force: true });
}

function addPackageToMainApplication(mainApplicationPath, androidPackage) {
  if (!fs.existsSync(mainApplicationPath)) return;
  let source = fs.readFileSync(mainApplicationPath, "utf8");
  if (source.includes("ChibiWidgetPackage")) return;

  const packageImport = `import ${androidPackage}.ChibiWidgetPackage`;
  if (source.includes("import com.facebook.react.PackageList")) {
    source = source.replace("import com.facebook.react.PackageList", `import com.facebook.react.PackageList\n${packageImport}`);
  } else {
    source = `${packageImport}\n${source}`;
  }

  const packagesExpression = "PackageList(this).packages.apply {";
  if (!source.includes(packagesExpression)) {
    throw new Error("Unable to register ChibiWidgetPackage in MainApplication.kt; Expo template structure changed.");
  }
  source = source.replace(packagesExpression, `${packagesExpression}\n      add(ChibiWidgetPackage())`);
  fs.writeFileSync(mainApplicationPath, source);
}

function withChibiWidget(config) {
  config = withWidgetManifest(config);
  config = withDangerousMod(config, ["android", async (modConfig) => {
    const androidPackage = modConfig.android?.package;
    if (!androidPackage) throw new Error("An Android package is required to generate the chibi widget.");

    const projectRoot = modConfig.modRequest.projectRoot;
    const androidRoot = modConfig.modRequest.platformProjectRoot;
    const nativeSource = path.join(projectRoot, "native-widget", "android");
    const appMain = path.join(androidRoot, "app", "src", "main");
    const kotlinDirectory = path.join(appMain, "java", ...androidPackage.split("."));

    ["ChibiWidgetModule.kt", "ChibiWidgetPackage.kt", "ChibiWidgetProvider.kt", "MomoOverlayService.kt"].forEach((file) => {
      writeTemplate(path.join(nativeSource, "kotlin", file), path.join(kotlinDirectory, file), androidPackage);
    });
    copyDirectory(path.join(nativeSource, "res"), path.join(appMain, "res"));

    const drawableDirectory = path.join(appMain, "res", "drawable-nodpi");
    fs.mkdirSync(drawableDirectory, { recursive: true });
    const fallbackAsset = path.join(projectRoot, "assets", "chibi", "idle.png");
    ["idle", "happy", "love", "sleepy", "excited", "shy", "sad", "walk", "climb", "fall", "pickedup", "rest"].forEach((mood) => {
      const source = path.join(projectRoot, "assets", "chibi", `${mood}.png`);
      fs.copyFileSync(fs.existsSync(source) ? source : fallbackAsset, path.join(drawableDirectory, `chibi_${mood}.png`));
    });

    addPackageToMainApplication(path.join(kotlinDirectory, "MainApplication.kt"), androidPackage);
    return modConfig;
  }]);
  return config;
}

module.exports = createRunOncePlugin(withChibiWidget, PLUGIN_NAME, "1.4.0");
