import { NativeModules, TurboModuleRegistry, type TurboModule } from 'react-native';

export interface OtpAutoVerifySpec extends TurboModule {
  getConstants(): { OTP_RECEIVED_EVENT: string };
  getHash(): Promise<ReadonlyArray<string>>;
  startSmsRetriever(): Promise<boolean>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

function getNativeModule(): OtpAutoVerifySpec {
  try {
    return TurboModuleRegistry.getEnforcing<OtpAutoVerifySpec>('OtpAutoVerify');
  } catch {
    const legacy = NativeModules.OtpAutoVerify;
    if (!legacy) {
      throw new Error(
        'OtpAutoVerify native module is not available. Ensure the library is properly linked.'
      );
    }
    return legacy as OtpAutoVerifySpec;
  }
}

export default getNativeModule();
