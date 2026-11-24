# Permanent Android release signing

The debug APK is for development only. GitHub-hosted runners create different debug signing keys, so debug APKs from different runs may not install as updates over one another.

For repeatable release updates, add these repository Actions secrets:

- `CHIBI_KEYSTORE_BASE64`: the complete one-line Base64 contents of the private keystore file supplied separately to the project owner.
- `CHIBI_KEYSTORE_PASSWORD`: the keystore/key password supplied separately to the project owner.

The alias is fixed as `chibi-release`.

In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**. Add each secret exactly as named above. Never commit the keystore, its Base64 contents, or its password to the repository.

After both secrets are configured, the next push to `main` will trigger the release build. The workflow will publish a `chibi-companion-release-apk` artifact signed with the permanent key.

Keep a secure backup of the keystore and password. Every future update must use the same key. If the key is lost, Android will not accept future APKs as updates to installations signed with it.

The signing key stabilizes app identity; it does not guarantee Google Play Protect approval. Because the app offers Accessibility and screen-capture features, Play Protect may still warn about sideloading. Do not disable Play Protect to install it. A Play-distributed release requires the applicable Play Console review and policy compliance.
