jest.mock('../NativeOtpAutoVerify', () => ({
  __esModule: true,
  default: {
    getConstants: () => ({ OTP_RECEIVED_EVENT: 'otpReceived' }),
  },
}));

import { extractOtp } from '../index';

describe('extractOtp', () => {
  it('extracts 6-digit OTP from message', () => {
    expect(extractOtp('Your code is 123456', 6)).toBe('123456');
    expect(extractOtp('123456 is your OTP', 6)).toBe('123456');
  });

  it('extracts 4-digit OTP from message', () => {
    expect(extractOtp('Code: 4521', 4)).toBe('4521');
  });

  it('extracts 5-digit OTP from message', () => {
    expect(extractOtp('OTP 98765 for login', 5)).toBe('98765');
  });

  it('extracts 7-digit OTP from message', () => {
    expect(extractOtp('Your code: 1234567', 7)).toBe('1234567');
  });

  it('extracts 8-digit OTP from message', () => {
    expect(extractOtp('Verify with 87654321', 8)).toBe('87654321');
  });

  it('returns null for empty or invalid input', () => {
    expect(extractOtp('', 6)).toBeNull();
    expect(extractOtp('no digits here', 6)).toBeNull();
  });

  it('defaults to 6 digits when numberOfDigits not provided', () => {
    expect(extractOtp('Use 555666')).toBe('555666');
  });

  it('returns null for whitespace-only string', () => {
    expect(extractOtp('   ', 6)).toBeNull();
    expect(extractOtp('\t\n', 6)).toBeNull();
  });

  it('trims input before matching', () => {
    expect(extractOtp('  123456  ', 6)).toBe('123456');
  });

  it('returns first match when multiple digit groups exist', () => {
    expect(extractOtp('Code 123456 and 654321', 6)).toBe('123456');
  });

  it('returns null for non-string input (type guard)', () => {
    expect(extractOtp(null as unknown as string, 6)).toBeNull();
    expect(extractOtp(undefined as unknown as string, 6)).toBeNull();
  });
});
