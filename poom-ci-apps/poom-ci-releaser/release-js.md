> structure `package.json` toutjours à la racine
> ne pas oublier la dernière ligne vide lors de la réécriture du fichier (même si normalement flexio-flow s'en charge)

```json
{
  "name": "@flexio-oss/hotballoon",
  "version": "0.7.0-dev", // version gérée par flexio-flow
  "devDependencies": { //ne jamais propager dans les dépendances de dev
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git"
  },
  "dependencies": { // version à propager avant release si présent
    "@flexio-oss/js-commons-bundle": "1.3.0-dev"
  }

}
```


### release 1
```json
{
  "name": "@flexio-oss/js-commons-bundle",
  "version": "1.3.0", // ICI
  "devDependencies": {
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git"
  }
}
```

### release 2

```json
{
  "name": "@flexio-oss/hotballoon",
  "version": "0.7.0", // ICI
  "devDependencies": {
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git"
  },
  "dependencies": {
    "@flexio-oss/js-commons-bundle": "1.3.0" // ICI
  }

}
```

### release 3

```json
{
  "name": "@flexio-corp/js-hotballoon-parent",
  "version": "1.4.0",// ICI
  "devDependencies": {
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git"
  },
  "dependencies": {
    "@flexio-oss/js-style-bundle": "1.4.0-dev",
    "@flexio-oss/hotballoon": "0.7.0", // ICI
    "@flexio-oss/js-commons-bundle": "1.3.0" // ICI
  }
}
```
### release 4

```json
{
  "name": "@flexio-corp/js-apis-bundle",
  "version": "1.3.0", // ICI
  "repository": "git@github.com:Flexio-corp/js-apis-bundle.git",
  "author": "Thomas Chatelain (https://github.com/TomAchT)",
  "devDependencies": {
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git",
    "@flexio-corp/component-commons-bundle": "1.0.0"
  },
  "dependencies": {
    "@flexio-corp/js-hotballoon-parent": "1.4.0-dev", // ICI
    "@flexio-corp/poom-l10n-formatter-spec": "1.0.0",
    "@flexio-corp/flexio-resource-view-client": "2.35.0",
    "@flexio-corp/flexio-resources-client": "2.35.0",
    "@flexio-corp/flexio-medias-client": "2.25.0",
    "@flexio-corp/flexio-ingredient-interpreter-spec": "2.33.0",
    "@flexio-corp/flexio-ingredient-api-client": "2.33.0",
    "@flexio-corp/flexio-standard-entity-ui-api-client": "2.35.0",
    "@flexio-corp/flexio-standard-paged-collection-ui-api-client": "2.35.0"
  }

}
```
### release 5
```json
{
  "name": "@flexio-corp/component-commons-bundle",
  "version": "1.3.0",// ICI
  "devDependencies": {
    "code-altimeter-js": "https://github.com/flexiooss/code-altimeter-js.git"
  },
  "dependencies": {
    "@flexio-corp/js-hotballoon-parent": "1.3.0",// ICI
    "@flexio-corp/js-apis-bundle": "1.3.0", // ICI
    "leaflet": "1.6.0",
    "tinymce": "5.2.1"
  }
}

```

