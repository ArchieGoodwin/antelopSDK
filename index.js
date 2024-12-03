import { NativeModules } from 'react-native'

const { Antelop } = NativeModules

export default class AntelopModule {
  static async initialize() {
    return await Antelop.initialize()
  }

  static async setDefaultAppPayment(enabled) {
    return await Antelop.setDefaultAppPayment(enabled)
  }

  static async isDefaultAppPayment() {
    return await Antelop.isDefaultAppPayment()
  }

  static async registerNfcStatusListener() {
    return await Antelop.registerNfcStatusListener()
  }

  static async requestNfcActivation() {
    return await Antelop.requestNfcActivation()
  }

  static async setForegroundPaymentPriority(enabled) {
    return await Antelop.setForegroundPaymentPriority(enabled)
  }

  static async isForegroundPaymentPriorityEnabled() {
    return await Antelop.isForegroundPaymentPriorityEnabled()
  }

  static async hasHceFeature() {
    return await Antelop.hasHceFeature()
  }

  static async hasNfcFeature() {
    return await Antelop.hasNfcFeature()
  }

  static async checkEligibility() {
    return await Antelop.checkEligibility()
  }

  static async launch(clientId, walletId, settingsProfileId, phoneNumber) {
    return await Antelop.launch(clientId, walletId, settingsProfileId, phoneNumber)
  }

  static async launchWithActivationCode(activationCode) {
    return await Antelop.launchWithActivationCode(activationCode)
  }

  static async clean() {
    return await Antelop.clean()
  }

  static async connect(passcode, cryptogram, cryptogramData) {
    return await Antelop.connect(passcode, cryptogram, cryptogramData)
  }

  static async logout() {
    return await Antelop.logout()
  }

  static async delete() {
    return await Antelop.delete()
  }

  static async getAuthenticationPatterns() {
    return await Antelop.getAuthenticationPatterns()
  }

  static async runSignature(patternName, deviceUuid, mobilePhone, appVersion) {
    return await Antelop.runSignature(patternName, deviceUuid, mobilePhone, appVersion)
  }

  static async isBiometricsActivated() {
    return await Antelop.isBiometricsActivated()
  }

  static async isBiometricsSupported() {
    return await Antelop.isBiometricsSupported()
  }

  static async activateBiometrics(passcode, cryptogram, cryptogramData) {
    return await Antelop.activateBiometrics(passcode, cryptogram, cryptogramData)
  }

  static async deactivateBiometrics(passcode, cryptogram, cryptogramData) {
    return await Antelop.deactivateBiometrics(passcode, cryptogram, cryptogramData)
  }

  static async runStrongSignatureWithPasscode(passcode, cryptogram, cryptogramData, deviceUuid, mobilePhone, appVersion) {
    return await Antelop.runStrongSignatureWithPasscode(passcode, cryptogram, cryptogramData, deviceUuid, mobilePhone, appVersion)
  }

  static async authenticateByConsent(scaType) {
    return await Antelop.authenticateByConsent(scaType)
  }

  static async authenticateByPasscode(passcode, cryptogram, cryptogramData, scaType) {
    return await Antelop.authenticateByPasscode(passcode, cryptogram, cryptogramData, scaType)
  }

  static async promptDeviceBiometrics(silent, scaType) {
    return await Antelop.promptDeviceBiometrics(silent, scaType)
  }

  static async getWalletId() {
    return await Antelop.getWalletId()
  }

  static async synchronizePasscode(passcode, cryptogram, cryptogramData) {
    return await Antelop.synchronizePasscode(passcode, cryptogram, cryptogramData)
  }

  static async checkPasscode(passcode, cryptogram, cryptogramData) {
    return await Antelop.checkPasscode(passcode, cryptogram, cryptogramData)
  }

  static async setDefaultCard(cardId) {
    return await Antelop.setDefaultCard(cardId)
  }

  static async setNextTransactionCard(cardId) {
    return await Antelop.setNextTransactionCard(cardId)
  }

  static async resetNextTransactionCard() {
    return await Antelop.resetNextTransactionCard()
  }

  static async authenticateHceTransaction() {
    return await Antelop.authenticateHceTransaction()
  }

  static async getEmulatedCards() {
    return await Antelop.getEmulatedCards()
  }

  static async getEmulatedCardsNumber() {
    return await Antelop.getEmulatedCardsNumber()
  }

  static async getHceTransactions(offset, limit) {
    return await Antelop.getHceTransactions(offset, limit)
  }

  static async checkIfHceTransactionOngoing() {
    return await Antelop.checkIfHceTransactionOngoing()
  }

  static async checkIfPushAuthenticationRequestWaiting() {
    return await Antelop.checkIfPushAuthenticationRequestWaiting()
  }

  static async cancelOngoingHceTransaction() {
    return await Antelop.cancelOngoingHceTransaction()
  }

  static async authenticatePushRequest() {
    return await Antelop.authenticatePushRequest()
  }

  static async cancelPushAuthenticationRequest() {
    return await Antelop.cancelPushAuthenticationRequest()
  }

  static async onTokenRefresh() {
    return await Antelop.onTokenRefresh()
  }

  static async onMessageReceived(remoteMessage) {
    return await Antelop.onMessageReceived(remoteMessage)
  }
}
