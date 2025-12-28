module.exports={
  "title": "RDI Documentation",
  "tagline": "Dependency Injection library with reactive capabilities, powered by Reactor",
  "url": "https://alex1304.github.io",
  "baseUrl": "/rdi/",
  "organizationName": "Alex1304",
  "projectName": "rdi",
  "scripts": [
    "https://buttons.github.io/buttons.js"
  ],
  "favicon": "img/favicon.ico",
  "customFields": {
    "javadocSite": "https://www.javadoc.io/doc/com.github.alex1304/rdi/latest/index.html",
    "repoUrl": "https://github.com/Alex1304/rdi"
  },
  "onBrokenLinks": "log",
  "onBrokenMarkdownLinks": "log",
  "presets": [
    [
      "@docusaurus/preset-classic",
      {
        "docs": {
          "showLastUpdateAuthor": true,
          "showLastUpdateTime": true,
          "path": "../docs",
          "sidebarPath": "./sidebars.json"
        },
        "blog": {},
        "theme": {
          "customCss": "./src/css/customTheme.css"
        }
      }
    ]
  ],
  "plugins": [],
  "themeConfig": {
    "navbar": {
      "title": "RDI Documentation",
      "logo": {
        "src": "img/logo.png"
      },
      "items": [
        {
          "to": "docs/intro",
          "label": "Docs",
          "position": "left"
        },
        {
          "href": "https://www.javadoc.io/doc/com.github.alex1304/rdi/latest/index.html",
          "label": "API",
          "position": "left"
        }
      ]
    },
    "image": "img/logo.png",
    "footer": {
      "links": [],
      "copyright": "Copyright © 2025 Alex1304 - Made with Docusaurus"
    }
  }
}