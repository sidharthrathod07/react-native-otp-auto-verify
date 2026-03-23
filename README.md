# react-native-otp-auto-verify

[![npm version](https://img.shields.io/npm/v/react-native-otp-auto-verify.svg?style=flat-square)](https://www.npmjs.com/package/react-native-otp-auto-verify) [![npm downloads](https://img.shields.io/npm/dm/react-native-otp-auto-verify.svg?style=flat-square)](https://www.npmjs.com/package/react-native-otp-auto-verify) [![license](https://img.shields.io/npm/l/react-native-otp-auto-verify.svg?style=flat-square)](https://github.com/kailas-rathod/react-native-otp-auto-verify/blob/main/LICENSE) [![typescript](https://img.shields.io/badge/TypeScript-Ready-blue.svg?style=flat-square)](https://www.typescriptlang.org/)

**react-native-otp-auto-verify** is a lightweight and secure React Native OTP auto-verification library for Android, built on the official Google SMS Retriever API. It enables automatic OTP detection without requiring READ_SMS or RECEIVE_SMS permissions, ensuring full Google Play Store compliance and enhanced user trust. Designed for modern authentication flows, this library is ideal for fintech apps, banking applications, e-commerce platforms, and secure login systems.

- **No permissions**: It requires zero SMS permissions from the user, making it compliant with strict Google Play Store policies.

With minimal dependencies and clean architecture, it integrates seamlessly into both React Native Old Architecture and the New Architecture (TurboModule) environments. The solution improves user experience by eliminating manual OTP entry on Android while maintaining strong server-side validation standards.

Works best for onboarding/login flows in **banking**, **fintech**, and authentication-heavy apps.
Supports both RN Old Architecture and React Native **New Architecture** (TurboModule).
**Android**: automatic OTP detection.

**iOS**: Uses iOS Security Code AutoFill (manual tap required).

**Connect:** [GitHub](https://github.com/kailas-rathod/react-native-otp-auto-verify) · [npm](https://www.npmjs.com/package/react-native-otp-auto-verify) · [Issues](https://github.com/kailas-rathod/react-native-otp-auto-verify/issues) · [License](https://github.com/kailas-rathod/react-native-otp-auto-verify/blob/main/LICENSE)

---

<img width="1536" height="1024" alt="otp" src="https://github.com/user-attachments/assets/e4908e99-e7d1-4a96-a6d2-b92c50090db0" />

## Features

- ✅ **Automatic OTP detection**: receives OTP from matching SMS and exposes it as `otp` (hook) or `extractedOtp` (listener)
- ✅ **No SMS permissions**: no access to inbox, avoids sensitive permissions and reduces compliance friction
- ✅ **App hash security**: OTP SMS must end with your **11-character hash** (only your app can receive it)
- ✅ **Hook + Imperative API**: `useOtpVerification()` for screens, `activateOtpListener()` for custom flows
- ✅ **TypeScript**: typed options and return values
- ✅ **New Architecture ready**: TurboModule implementation; works with Old Architecture too

---

## Platform Support

| Platform | Support        | Notes                                                 |
| -------- | -------------- | ----------------------------------------------------- |
| Android  | ✅             | Requires Google Play Services on device               |
| iOS      | ✅ Native Only | Uses iOS Security Code AutoFill (manual tap required) |

## Requirements

- React Native: **0.60+** (autolinking)
- Android: **minSdkVersion 24+**
- **TurboModule**: The library uses codegen/TurboModule. The native module must be built and linked. If it is not registered (e.g. build issue or unsupported setup), importing the library will throw at load time.

## Installation

```sh
npm install react-native-otp-auto-verify
```

# or

```sh
yarn add react-native-otp-auto-verify
```

# or

```sh
pnpm add react-native-otp-auto-verify
```

## Usage

### 1) Get your app hash

SMS Retriever only delivers messages that include your **11-character app hash**.

```ts
import { getHash } from 'react-native-otp-auto-verify';

const hashes = await getHash();
const appHash = hashes[0]; // send this to your backend
```

### 2) Format your OTP SMS

Your backend **must** include the app hash at the **end** of the SMS.

Requirements:

- Message must be **≤ 140 bytes**
- Must contain a **4–8 digit** OTP
- Must **end with** the app hash from `getHash()`

Recommended format:

```
Dear Kailas Rathod, 321500 is your OTP for mobile authentication. This OTP is valid for the next 15 minutes. Please DO NOT share it with anyone.
uW87Uq6teXc
```

Note: You do not need `<#>` at the start of the message.

### 3) Hook usage (recommended)

Start listening only while the OTP screen is visible (foreground).

```tsx
import React from 'react';
import { Text, View } from 'react-native';
import { useOtpVerification } from 'react-native-otp-auto-verify';

export function OtpScreen() {
  const { hashCode, otp, timeoutError, error, startListening, stopListening } =
    useOtpVerification({ numberOfDigits: 6 });

  React.useEffect(() => {
    void startListening();
    return () => stopListening();
  }, [startListening, stopListening]);

  return (
    <View>
      {!!hashCode && <Text>Hash: {hashCode}</Text>}
      {!!otp && <Text>OTP: {otp}</Text>}
      {timeoutError && <Text>Timeout. Tap resend and try again.</Text>}
      {!!error && <Text>Error: {error.message}</Text>}
    </View>
  );
}
```

### 4) Imperative usage

```ts
import {
  activateOtpListener,
  removeListener,
} from 'react-native-otp-auto-verify';

const sub = await activateOtpListener(
  (sms, extractedOtp) => {
    if (extractedOtp) {
      console.log('OTP:', extractedOtp);
    }
  },
  { numberOfDigits: 6 }
);

// cleanup
sub.remove();
// or
removeListener();
```

🔹 Step 1 – Start OTP Listener

```ts
import { useOtpVerification } from 'react-native-otp-auto-verify';

const { startListening, stopListening, otp } = useOtpVerification();

useEffect(() => {
  startListening();

  return () => stopListening();
}, []);
```

## Create OTP Screen (Recommended Hook Method)

```ts
import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, Button, Platform } from 'react-native';
import { useOtpVerification } from 'react-native-otp-auto-verify';

const OtpScreen = () => {
  const [otpValue, setOtpValue] = useState('');

  const { otp, hashCode, timeoutError, error, startListening, stopListening } =
    useOtpVerification({ numberOfDigits: 6 });

  // Start listener when screen opens
  useEffect(() => {
    if (Platform.OS === 'android') {
      startListening();
    }

    return () => {
      stopListening();
    };
  }, []);

  // Auto verify when OTP received
  useEffect(() => {
    if (otp) {
      setOtpValue(otp);
      verifyOtp(otp);
    }
  }, [otp]);

  const verifyOtp = async (code: string) => {
    console.log('Verifying OTP:', code);

    // Call your backend API here
    // await api.post('/verify-otp', { otp: code })
  };

  return (
    <View style={{ padding: 20 }}>
      <Text>Enter OTP</Text>

      <TextInput
        value={otpValue}
        onChangeText={setOtpValue}
        keyboardType="number-pad"
        maxLength={6}
        textContentType="oneTimeCode"
        autoComplete="sms-otp"
        style={{
          borderWidth: 1,
          padding: 12,
          marginVertical: 12,
        }}
      />

      <Button title="Verify" onPress={() => verifyOtp(otpValue)} />

      {timeoutError && (
        <Text style={{ color: 'red' }}>Timeout. Please resend OTP.</Text>
      )}

      {error && <Text style={{ color: 'red' }}>Error: {error.message}</Text>}
    </View>
  );
};

export default OtpScreen;
```

# Start OTP Listener in Screen

```ts
import React, { useEffect, useState } from 'react';
import { View, TextInput, Text } from 'react-native';
import { useOtpVerification } from 'react-native-otp-auto-verify';

export default function OtpScreen() {
  const [otpValue, setOtpValue] = useState('');

  const { otp, startListening, stopListening } = useOtpVerification({
    numberOfDigits: 6,
  });

  useEffect(() => {
    startListening(); // Start listening

    return () => {
      stopListening(); // Cleanup
    };
  }, []);

  useEffect(() => {
    if (otp) {
      setOtpValue(otp); // OTP automatically retrieved here
      console.log('Retrieved OTP:', otp);
    }
  }, [otp]);

  return (
    <View>
      <TextInput
        value={otpValue}
        onChangeText={setOtpValue}
        keyboardType="number-pad"
        maxLength={6}
      />
    </View>
  );
}
```

# iOS OTP AutoFill (Native)

iOS does **not** allow third-party libraries to read SMS messages.

Automatic SMS reading is restricted by Apple for privacy and security reasons.  
Instead, iOS provides a native feature called **Security Code AutoFill**, which suggests the OTP above the keyboard when properly configured.

This library does **not** auto-read OTP on iOS.

---

## How iOS OTP AutoFill Works

1. User receives an SMS containing an OTP.
2. iOS detects the code.
3. The OTP appears above the keyboard.
4. User taps the suggestion.
5. The code fills automatically into the input field.

No SMS permissions required.

---

## React Native Setup

Use the following configuration in your OTP input field:

```tsx
<TextInput
  style={styles.input}
  keyboardType="number-pad"
  textContentType="oneTimeCode"
  autoComplete="sms-otp"
  importantForAutofill="yes"
  maxLength={6}
/>
```

## react-native-otp-auto-verify Architecture Flow

<img width="1536" height="1024" alt="react-native-otp-auto-verify Architecture Flow" src="https://github.com/user-attachments/assets/11582523-81cb-4904-9de0-56af05b3a3b4" />

## API Reference

### `useOtpVerification(options?)`

Use this on your OTP screen. It manages:

- getting the app hash (`hashCode`)
- starting/stopping the SMS Retriever listener
- extracting OTP and exposing it as `otp`

| Property         | Type                  | Default | Description                              |
| ---------------- | --------------------- | ------- | ---------------------------------------- |
| `numberOfDigits` | `4 \| 5 \| 6 \| 7 \| 8` | `6`     | OTP length to extract                    |
| `hashCode`       | `string`              | `''`    | App hash (send to backend)               |
| `otp`            | `string \| null`      | `null`  | Extracted OTP                            |
| `sms`            | `string \| null`      | `null`  | Full SMS text                            |
| `timeoutError`   | `boolean`             | `false` | Timeout occurred                         |
| `error`          | `Error \| null`       | `null`  | Set when getHash or startListening fails |
| `startListening` | `() => Promise<void>` | —       | Start listening                          |
| `stopListening`  | `() => void`          | —       | Stop listening                           |

### `getHash(): Promise<string[]>`

Android only. On iOS returns `[]`.

### `activateOtpListener(handler, options?): Promise<{ remove(): void }>`

Android only. Throws on iOS.

### `removeListener(): void`

Android only. Removes native listeners.

### `extractOtp(sms: string, numberOfDigits?: 4 | 5 | 6 | 7 | 8): string | null`

Pure helper to extract the OTP from an SMS string.

## Timeout behavior

SMS Retriever waits up to **5 minutes**. When it times out:

- `timeoutError` becomes `true`
- call `startListening()` again to retry

## React Native New Architecture

This library is built with **TurboModules** and fully supports React Native's **New Architecture**.

### What is the New Architecture?

The New Architecture (also known as Fabric + TurboModules) is React Native's new rendering system and native module architecture that provides:

- Better performance and type safety
- Synchronous native module calls
- Improved interoperability with native code

## ✨ Feature Comparison

| Feature                 | react-native-otp-auto-verify | Other packages     |
| ----------------------- | ---------------------------- | ------------------ |
| SMS Retriever API       | ✅ Yes                       | ✅ Yes             |
| Requires SMS Permission | ❌ No                        | ❌ No              |
| TurboModule Support     | ✅ Yes                       | ❌ Usually No      |
| TypeScript Support      | ✅ Full                      | ⚠️ Partial         |
| Hook API                | ✅ `useOtpVerification`      | ❌ Not Available   |
| App Hash Utility        | ✅ Built-in                  | ⚠️ Basic           |
| Architecture Ready      | ✅ Old + New                 | ⚠️ Mostly Old Only |
| Maintenance             | ✅ Actively Maintained       | ⚠️ Varies          |

### Enabling New Architecture

The library works automatically with both **Old Architecture** and **New Architecture**. No code changes needed.

**For Android:**

Enable New Architecture in `android/gradle.properties`:

```properties
newArchEnabled=true
```

**For iOS:**

Enable New Architecture in `ios/Podfile`:

```ruby
use_react_native!(
  :fabric_enabled => true,
  # ... other options
)
```

Or set the environment variable:

```sh
RCT_NEW_ARCH_ENABLED=1 pod install
```

### Compatibility

- ✅ Works with **Old Architecture** (React Native < 0.68)
- ✅ Works with **New Architecture** (React Native 0.68+)
- ✅ Automatically detects and uses the correct architecture
- ✅ No breaking changes when migrating

## Troubleshooting

- OTP not detected
  - Ensure the SMS **ends with** the app hash
  - Keep the SMS **≤ 140 bytes**
  - Match `numberOfDigits` to your OTP length
  - Keep the app in **foreground**
- Listener fails to start
  - Ensure Google Play services are available on the device
  - Avoid multiple active listeners at once

## Example app

See [`./example`](./example).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Publishing

Maintainers: see [RELEASE_CHECKLIST.md](./RELEASE_CHECKLIST.md) before publishing a new version to npm.

## Keywords

react native otp auto verify, react native sms retriever api, automatic otp detection android, react native otp autofill, sms retriever react native, otp verification library react native, google play compliant otp library

## License

[MIT](./LICENSE) — see [LICENSE](./LICENSE) in the repo.
