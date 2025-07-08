package com.cehpoint.netwin.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DataStoreManager"
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val USER_PROFILE_PIC = stringPreferencesKey("user_profile_pic")
        private val USER_TOKEN = stringPreferencesKey("user_token")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val USER_STATUS = stringPreferencesKey("user_status")
        private val USER_CREATED_AT = stringPreferencesKey("user_created_at")
        private val USER_UPDATED_AT = stringPreferencesKey("user_updated_at")
        private val USER_LAST_LOGIN = stringPreferencesKey("user_last_login")
        private val USER_LAST_LOGIN_IP = stringPreferencesKey("user_last_login_ip")
        private val USER_LAST_LOGIN_DEVICE = stringPreferencesKey("user_last_login_device")
        private val USER_LAST_LOGIN_LOCATION = stringPreferencesKey("user_last_login_location")
        private val USER_LAST_LOGIN_BROWSER = stringPreferencesKey("user_last_login_browser")
        private val USER_LAST_LOGIN_OS = stringPreferencesKey("user_last_login_os")
        private val USER_LAST_LOGIN_PLATFORM = stringPreferencesKey("user_last_login_platform")
        private val USER_LAST_LOGIN_APP_VERSION = stringPreferencesKey("user_last_login_app_version")
        private val USER_LAST_LOGIN_APP_BUILD = stringPreferencesKey("user_last_login_app_build")
        private val USER_LAST_LOGIN_APP_CHANNEL = stringPreferencesKey("user_last_login_app_channel")
        private val USER_LAST_LOGIN_APP_ENVIRONMENT = stringPreferencesKey("user_last_login_app_environment")
        private val USER_LAST_LOGIN_APP_DEVICE_ID = stringPreferencesKey("user_last_login_app_device_id")
        private val USER_LAST_LOGIN_APP_DEVICE_NAME = stringPreferencesKey("user_last_login_app_device_name")
        private val USER_LAST_LOGIN_APP_DEVICE_MODEL = stringPreferencesKey("user_last_login_app_device_model")
        private val USER_LAST_LOGIN_APP_DEVICE_MANUFACTURER = stringPreferencesKey("user_last_login_app_device_manufacturer")
        private val USER_LAST_LOGIN_APP_DEVICE_BRAND = stringPreferencesKey("user_last_login_app_device_brand")
        private val USER_LAST_LOGIN_APP_DEVICE_PRODUCT = stringPreferencesKey("user_last_login_app_device_product")
        private val USER_LAST_LOGIN_APP_DEVICE_BOARD = stringPreferencesKey("user_last_login_app_device_board")
        private val USER_LAST_LOGIN_APP_DEVICE_HARDWARE = stringPreferencesKey("user_last_login_app_device_hardware")
        private val USER_LAST_LOGIN_APP_DEVICE_FINGERPRINT = stringPreferencesKey("user_last_login_app_device_fingerprint")
        private val USER_LAST_LOGIN_APP_DEVICE_SERIAL = stringPreferencesKey("user_last_login_app_device_serial")
        private val USER_LAST_LOGIN_APP_DEVICE_IMEI = stringPreferencesKey("user_last_login_app_device_imei")
        private val USER_LAST_LOGIN_APP_DEVICE_MEID = stringPreferencesKey("user_last_login_app_device_meid")
        private val USER_LAST_LOGIN_APP_DEVICE_MAC = stringPreferencesKey("user_last_login_app_device_mac")
        private val USER_LAST_LOGIN_APP_DEVICE_BLUETOOTH_MAC = stringPreferencesKey("user_last_login_app_device_bluetooth_mac")
        private val USER_LAST_LOGIN_APP_DEVICE_WIFI_MAC = stringPreferencesKey("user_last_login_app_device_wifi_mac")
        private val USER_LAST_LOGIN_APP_DEVICE_ANDROID_ID = stringPreferencesKey("user_last_login_app_device_android_id")
        private val USER_LAST_LOGIN_APP_DEVICE_ADVERTISING_ID = stringPreferencesKey("user_last_login_app_device_advertising_id")
        private val USER_LAST_LOGIN_APP_DEVICE_INSTALLATION_ID = stringPreferencesKey("user_last_login_app_device_installation_id")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TOKEN = stringPreferencesKey("user_last_login_app_device_push_token")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_PROVIDER = stringPreferencesKey("user_last_login_app_device_push_provider")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_CHANNEL = stringPreferencesKey("user_last_login_app_device_push_channel")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TOPIC = stringPreferencesKey("user_last_login_app_device_push_topic")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TAGS = stringPreferencesKey("user_last_login_app_device_push_tags")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_SOUND = stringPreferencesKey("user_last_login_app_device_push_sound")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_BADGE = stringPreferencesKey("user_last_login_app_device_push_badge")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_ALERT = stringPreferencesKey("user_last_login_app_device_push_alert")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_CONTENT_AVAILABLE = stringPreferencesKey("user_last_login_app_device_push_content_available")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_MUTABLE_CONTENT = stringPreferencesKey("user_last_login_app_device_push_mutable_content")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_CATEGORY = stringPreferencesKey("user_last_login_app_device_push_category")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_THREAD_ID = stringPreferencesKey("user_last_login_app_device_push_thread_id")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ID = stringPreferencesKey("user_last_login_app_device_push_target_content_id")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TYPE = stringPreferencesKey("user_last_login_app_device_push_target_content_type")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TITLE = stringPreferencesKey("user_last_login_app_device_push_target_content_title")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BODY = stringPreferencesKey("user_last_login_app_device_push_target_content_body")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_URL = stringPreferencesKey("user_last_login_app_device_push_target_content_url")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_IMAGE = stringPreferencesKey("user_last_login_app_device_push_target_content_image")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ICON = stringPreferencesKey("user_last_login_app_device_push_target_content_icon")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_SOUND = stringPreferencesKey("user_last_login_app_device_push_target_content_sound")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BADGE = stringPreferencesKey("user_last_login_app_device_push_target_content_badge")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ALERT = stringPreferencesKey("user_last_login_app_device_push_target_content_alert")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CONTENT_AVAILABLE = stringPreferencesKey("user_last_login_app_device_push_target_content_content_available")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_MUTABLE_CONTENT = stringPreferencesKey("user_last_login_app_device_push_target_content_mutable_content")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CATEGORY = stringPreferencesKey("user_last_login_app_device_push_target_content_category")
        private val USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_THREAD_ID = stringPreferencesKey("user_last_login_app_device_push_target_content_thread_id")
    }

    // User ID
    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        val userId = preferences[USER_ID] ?: ""
        Log.d(TAG, "DataStore - Reading userId: $userId")
        userId
    }

    suspend fun setUserId(userId: String) {
        Log.d(TAG, "DataStore - Setting userId: $userId")
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
        Log.d(TAG, "DataStore - userId set successfully")
    }

    suspend fun getUserId(): String {
        return context.dataStore.data.first()[USER_ID] ?: ""
    }

    // User Name
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        val userName = preferences[USER_NAME] ?: ""
        Log.d(TAG, "DataStore - Reading userName: $userName")
        userName
    }

    suspend fun setUserName(userName: String) {
        Log.d(TAG, "DataStore - Setting userName: $userName")
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = userName
        }
        Log.d(TAG, "DataStore - userName set successfully")
    }

    // User Email
    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        val userEmail = preferences[USER_EMAIL] ?: ""
        Log.d(TAG, "DataStore - Reading userEmail: $userEmail")
        userEmail
    }

    suspend fun setUserEmail(userEmail: String) {
        Log.d(TAG, "DataStore - Setting userEmail: $userEmail")
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = userEmail
        }
        Log.d(TAG, "DataStore - userEmail set successfully")
    }

    suspend fun getUserEmail(): String {
        return context.dataStore.data.first()[USER_EMAIL] ?: ""
    }

    // User Phone
    val userPhone: Flow<String> = context.dataStore.data.map { preferences ->
        val userPhone = preferences[USER_PHONE] ?: ""
        Log.d(TAG, "DataStore - Reading userPhone: $userPhone")
        userPhone
    }

    suspend fun setUserPhone(userPhone: String) {
        Log.d(TAG, "DataStore - Setting userPhone: $userPhone")
        context.dataStore.edit { preferences ->
            preferences[USER_PHONE] = userPhone
        }
        Log.d(TAG, "DataStore - userPhone set successfully")
    }

    // User Profile Pic
    val userProfilePic: Flow<String> = context.dataStore.data.map { preferences ->
        val userProfilePic = preferences[USER_PROFILE_PIC] ?: ""
        Log.d(TAG, "DataStore - Reading userProfilePic: $userProfilePic")
        userProfilePic
    }

    suspend fun setUserProfilePic(userProfilePic: String) {
        Log.d(TAG, "DataStore - Setting userProfilePic: $userProfilePic")
        context.dataStore.edit { preferences ->
            preferences[USER_PROFILE_PIC] = userProfilePic
        }
        Log.d(TAG, "DataStore - userProfilePic set successfully")
    }

    // User Token
    val userToken: Flow<String> = context.dataStore.data.map { preferences ->
        val userToken = preferences[USER_TOKEN] ?: ""
        Log.d(TAG, "DataStore - Reading userToken: ${userToken.take(20)}...")
        userToken
    }

    suspend fun setUserToken(userToken: String) {
        Log.d(TAG, "DataStore - Setting userToken: ${userToken.take(20)}...")
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN] = userToken
        }
        Log.d(TAG, "DataStore - userToken set successfully")
    }

    // User Role
    val userRole: Flow<String> = context.dataStore.data.map { preferences ->
        val userRole = preferences[USER_ROLE] ?: ""
        Log.d(TAG, "DataStore - Reading userRole: $userRole")
        userRole
    }

    suspend fun setUserRole(userRole: String) {
        Log.d(TAG, "DataStore - Setting userRole: $userRole")
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE] = userRole
        }
        Log.d(TAG, "DataStore - userRole set successfully")
    }

    // User Status
    val userStatus: Flow<String> = context.dataStore.data.map { preferences ->
        val userStatus = preferences[USER_STATUS] ?: ""
        Log.d(TAG, "DataStore - Reading userStatus: $userStatus")
        userStatus
    }

    suspend fun setUserStatus(userStatus: String) {
        Log.d(TAG, "DataStore - Setting userStatus: $userStatus")
        context.dataStore.edit { preferences ->
            preferences[USER_STATUS] = userStatus
        }
        Log.d(TAG, "DataStore - userStatus set successfully")
    }

    // User Created At
    val userCreatedAt: Flow<String> = context.dataStore.data.map { preferences ->
        val userCreatedAt = preferences[USER_CREATED_AT] ?: ""
        Log.d(TAG, "DataStore - Reading userCreatedAt: $userCreatedAt")
        userCreatedAt
    }

    suspend fun setUserCreatedAt(userCreatedAt: String) {
        Log.d(TAG, "DataStore - Setting userCreatedAt: $userCreatedAt")
        context.dataStore.edit { preferences ->
            preferences[USER_CREATED_AT] = userCreatedAt
        }
        Log.d(TAG, "DataStore - userCreatedAt set successfully")
    }

    // User Updated At
    val userUpdatedAt: Flow<String> = context.dataStore.data.map { preferences ->
        val userUpdatedAt = preferences[USER_UPDATED_AT] ?: ""
        Log.d(TAG, "DataStore - Reading userUpdatedAt: $userUpdatedAt")
        userUpdatedAt
    }

    suspend fun setUserUpdatedAt(userUpdatedAt: String) {
        Log.d(TAG, "DataStore - Setting userUpdatedAt: $userUpdatedAt")
        context.dataStore.edit { preferences ->
            preferences[USER_UPDATED_AT] = userUpdatedAt
        }
        Log.d(TAG, "DataStore - userUpdatedAt set successfully")
    }

    // User Last Login
    val userLastLogin: Flow<String> = context.dataStore.data.map { preferences ->
        val userLastLogin = preferences[USER_LAST_LOGIN] ?: ""
        Log.d(TAG, "DataStore - Reading userLastLogin: $userLastLogin")
        userLastLogin
    }

    suspend fun setUserLastLogin(userLastLogin: String) {
        Log.d(TAG, "DataStore - Setting userLastLogin: $userLastLogin")
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN] = userLastLogin
        }
        Log.d(TAG, "DataStore - userLastLogin set successfully")
    }

    // User Last Login IP
    val userLastLoginIp: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_IP] ?: ""
    }

    suspend fun setUserLastLoginIp(userLastLoginIp: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_IP] = userLastLoginIp
        }
    }

    // User Last Login Device
    val userLastLoginDevice: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_DEVICE] ?: ""
    }

    suspend fun setUserLastLoginDevice(userLastLoginDevice: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_DEVICE] = userLastLoginDevice
        }
    }

    // User Last Login Location
    val userLastLoginLocation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_LOCATION] ?: ""
    }

    suspend fun setUserLastLoginLocation(userLastLoginLocation: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_LOCATION] = userLastLoginLocation
        }
    }

    // User Last Login Browser
    val userLastLoginBrowser: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_BROWSER] ?: ""
    }

    suspend fun setUserLastLoginBrowser(userLastLoginBrowser: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_BROWSER] = userLastLoginBrowser
        }
    }

    // User Last Login OS
    val userLastLoginOs: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_OS] ?: ""
    }

    suspend fun setUserLastLoginOs(userLastLoginOs: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_OS] = userLastLoginOs
        }
    }

    // User Last Login Platform
    val userLastLoginPlatform: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_PLATFORM] ?: ""
    }

    suspend fun setUserLastLoginPlatform(userLastLoginPlatform: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_PLATFORM] = userLastLoginPlatform
        }
    }

    // User Last Login App Version
    val userLastLoginAppVersion: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_VERSION] ?: ""
    }

    suspend fun setUserLastLoginAppVersion(userLastLoginAppVersion: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_VERSION] = userLastLoginAppVersion
        }
    }

    // User Last Login App Build
    val userLastLoginAppBuild: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_BUILD] ?: ""
    }

    suspend fun setUserLastLoginAppBuild(userLastLoginAppBuild: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_BUILD] = userLastLoginAppBuild
        }
    }

    // User Last Login App Channel
    val userLastLoginAppChannel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_CHANNEL] ?: ""
    }

    suspend fun setUserLastLoginAppChannel(userLastLoginAppChannel: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_CHANNEL] = userLastLoginAppChannel
        }
    }

    // User Last Login App Environment
    val userLastLoginAppEnvironment: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_ENVIRONMENT] ?: ""
    }

    suspend fun setUserLastLoginAppEnvironment(userLastLoginAppEnvironment: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_ENVIRONMENT] = userLastLoginAppEnvironment
        }
    }

    // User Last Login App Device ID
    val userLastLoginAppDeviceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceId(userLastLoginAppDeviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_ID] = userLastLoginAppDeviceId
        }
    }

    // User Last Login App Device Name
    val userLastLoginAppDeviceName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_NAME] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceName(userLastLoginAppDeviceName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_NAME] = userLastLoginAppDeviceName
        }
    }

    // User Last Login App Device Model
    val userLastLoginAppDeviceModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_MODEL] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceModel(userLastLoginAppDeviceModel: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_MODEL] = userLastLoginAppDeviceModel
        }
    }

    // User Last Login App Device Manufacturer
    val userLastLoginAppDeviceManufacturer: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_MANUFACTURER] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceManufacturer(userLastLoginAppDeviceManufacturer: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_MANUFACTURER] = userLastLoginAppDeviceManufacturer
        }
    }

    // User Last Login App Device Brand
    val userLastLoginAppDeviceBrand: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_BRAND] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceBrand(userLastLoginAppDeviceBrand: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_BRAND] = userLastLoginAppDeviceBrand
        }
    }

    // User Last Login App Device Product
    val userLastLoginAppDeviceProduct: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PRODUCT] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceProduct(userLastLoginAppDeviceProduct: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PRODUCT] = userLastLoginAppDeviceProduct
        }
    }

    // User Last Login App Device Board
    val userLastLoginAppDeviceBoard: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_BOARD] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceBoard(userLastLoginAppDeviceBoard: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_BOARD] = userLastLoginAppDeviceBoard
        }
    }

    // User Last Login App Device Hardware
    val userLastLoginAppDeviceHardware: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_HARDWARE] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceHardware(userLastLoginAppDeviceHardware: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_HARDWARE] = userLastLoginAppDeviceHardware
        }
    }

    // User Last Login App Device Fingerprint
    val userLastLoginAppDeviceFingerprint: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_FINGERPRINT] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceFingerprint(userLastLoginAppDeviceFingerprint: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_FINGERPRINT] = userLastLoginAppDeviceFingerprint
        }
    }

    // User Last Login App Device Serial
    val userLastLoginAppDeviceSerial: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_SERIAL] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceSerial(userLastLoginAppDeviceSerial: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_SERIAL] = userLastLoginAppDeviceSerial
        }
    }

    // User Last Login App Device IMEI
    val userLastLoginAppDeviceImei: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_IMEI] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceImei(userLastLoginAppDeviceImei: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_IMEI] = userLastLoginAppDeviceImei
        }
    }

    // User Last Login App Device MEID
    val userLastLoginAppDeviceMeid: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_MEID] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceMeid(userLastLoginAppDeviceMeid: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_MEID] = userLastLoginAppDeviceMeid
        }
    }

    // User Last Login App Device MAC
    val userLastLoginAppDeviceMac: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_MAC] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceMac(userLastLoginAppDeviceMac: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_MAC] = userLastLoginAppDeviceMac
        }
    }

    // User Last Login App Device Bluetooth MAC
    val userLastLoginAppDeviceBluetoothMac: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_BLUETOOTH_MAC] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceBluetoothMac(userLastLoginAppDeviceBluetoothMac: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_BLUETOOTH_MAC] = userLastLoginAppDeviceBluetoothMac
        }
    }

    // User Last Login App Device WiFi MAC
    val userLastLoginAppDeviceWifiMac: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_WIFI_MAC] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceWifiMac(userLastLoginAppDeviceWifiMac: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_WIFI_MAC] = userLastLoginAppDeviceWifiMac
        }
    }

    // User Last Login App Device Android ID
    val userLastLoginAppDeviceAndroidId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_ANDROID_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceAndroidId(userLastLoginAppDeviceAndroidId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_ANDROID_ID] = userLastLoginAppDeviceAndroidId
        }
    }

    // User Last Login App Device Advertising ID
    val userLastLoginAppDeviceAdvertisingId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_ADVERTISING_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceAdvertisingId(userLastLoginAppDeviceAdvertisingId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_ADVERTISING_ID] = userLastLoginAppDeviceAdvertisingId
        }
    }

    // User Last Login App Device Installation ID
    val userLastLoginAppDeviceInstallationId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_INSTALLATION_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDeviceInstallationId(userLastLoginAppDeviceInstallationId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_INSTALLATION_ID] = userLastLoginAppDeviceInstallationId
        }
    }

    // User Last Login App Device Push Token
    val userLastLoginAppDevicePushToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TOKEN] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushToken(userLastLoginAppDevicePushToken: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TOKEN] = userLastLoginAppDevicePushToken
        }
    }

    // User Last Login App Device Push Provider
    val userLastLoginAppDevicePushProvider: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_PROVIDER] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushProvider(userLastLoginAppDevicePushProvider: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_PROVIDER] = userLastLoginAppDevicePushProvider
        }
    }

    // User Last Login App Device Push Channel
    val userLastLoginAppDevicePushChannel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CHANNEL] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushChannel(userLastLoginAppDevicePushChannel: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CHANNEL] = userLastLoginAppDevicePushChannel
        }
    }

    // User Last Login App Device Push Topic
    val userLastLoginAppDevicePushTopic: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TOPIC] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTopic(userLastLoginAppDevicePushTopic: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TOPIC] = userLastLoginAppDevicePushTopic
        }
    }

    // User Last Login App Device Push Tags
    val userLastLoginAppDevicePushTags: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TAGS] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTags(userLastLoginAppDevicePushTags: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TAGS] = userLastLoginAppDevicePushTags
        }
    }

    // User Last Login App Device Push Sound
    val userLastLoginAppDevicePushSound: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_SOUND] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushSound(userLastLoginAppDevicePushSound: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_SOUND] = userLastLoginAppDevicePushSound
        }
    }

    // User Last Login App Device Push Badge
    val userLastLoginAppDevicePushBadge: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_BADGE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushBadge(userLastLoginAppDevicePushBadge: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_BADGE] = userLastLoginAppDevicePushBadge
        }
    }

    // User Last Login App Device Push Alert
    val userLastLoginAppDevicePushAlert: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_ALERT] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushAlert(userLastLoginAppDevicePushAlert: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_ALERT] = userLastLoginAppDevicePushAlert
        }
    }

    // User Last Login App Device Push Content Available
    val userLastLoginAppDevicePushContentAvailable: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CONTENT_AVAILABLE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushContentAvailable(userLastLoginAppDevicePushContentAvailable: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CONTENT_AVAILABLE] = userLastLoginAppDevicePushContentAvailable
        }
    }

    // User Last Login App Device Push Mutable Content
    val userLastLoginAppDevicePushMutableContent: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_MUTABLE_CONTENT] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushMutableContent(userLastLoginAppDevicePushMutableContent: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_MUTABLE_CONTENT] = userLastLoginAppDevicePushMutableContent
        }
    }

    // User Last Login App Device Push Category
    val userLastLoginAppDevicePushCategory: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CATEGORY] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushCategory(userLastLoginAppDevicePushCategory: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_CATEGORY] = userLastLoginAppDevicePushCategory
        }
    }

    // User Last Login App Device Push Thread ID
    val userLastLoginAppDevicePushThreadId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_THREAD_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushThreadId(userLastLoginAppDevicePushThreadId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_THREAD_ID] = userLastLoginAppDevicePushThreadId
        }
    }

    // User Last Login App Device Push Target Content ID
    val userLastLoginAppDevicePushTargetContentId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentId(userLastLoginAppDevicePushTargetContentId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ID] = userLastLoginAppDevicePushTargetContentId
        }
    }

    // User Last Login App Device Push Target Content Type
    val userLastLoginAppDevicePushTargetContentType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TYPE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentType(userLastLoginAppDevicePushTargetContentType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TYPE] = userLastLoginAppDevicePushTargetContentType
        }
    }

    // User Last Login App Device Push Target Content Title
    val userLastLoginAppDevicePushTargetContentTitle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TITLE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentTitle(userLastLoginAppDevicePushTargetContentTitle: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_TITLE] = userLastLoginAppDevicePushTargetContentTitle
        }
    }

    // User Last Login App Device Push Target Content Body
    val userLastLoginAppDevicePushTargetContentBody: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BODY] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentBody(userLastLoginAppDevicePushTargetContentBody: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BODY] = userLastLoginAppDevicePushTargetContentBody
        }
    }

    // User Last Login App Device Push Target Content URL
    val userLastLoginAppDevicePushTargetContentUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_URL] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentUrl(userLastLoginAppDevicePushTargetContentUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_URL] = userLastLoginAppDevicePushTargetContentUrl
        }
    }

    // User Last Login App Device Push Target Content Image
    val userLastLoginAppDevicePushTargetContentImage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_IMAGE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentImage(userLastLoginAppDevicePushTargetContentImage: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_IMAGE] = userLastLoginAppDevicePushTargetContentImage
        }
    }

    // User Last Login App Device Push Target Content Icon
    val userLastLoginAppDevicePushTargetContentIcon: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ICON] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentIcon(userLastLoginAppDevicePushTargetContentIcon: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ICON] = userLastLoginAppDevicePushTargetContentIcon
        }
    }

    // User Last Login App Device Push Target Content Sound
    val userLastLoginAppDevicePushTargetContentSound: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_SOUND] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentSound(userLastLoginAppDevicePushTargetContentSound: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_SOUND] = userLastLoginAppDevicePushTargetContentSound
        }
    }

    // User Last Login App Device Push Target Content Badge
    val userLastLoginAppDevicePushTargetContentBadge: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BADGE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentBadge(userLastLoginAppDevicePushTargetContentBadge: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_BADGE] = userLastLoginAppDevicePushTargetContentBadge
        }
    }

    // User Last Login App Device Push Target Content Alert
    val userLastLoginAppDevicePushTargetContentAlert: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ALERT] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentAlert(userLastLoginAppDevicePushTargetContentAlert: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_ALERT] = userLastLoginAppDevicePushTargetContentAlert
        }
    }

    // User Last Login App Device Push Target Content Content Available
    val userLastLoginAppDevicePushTargetContentContentAvailable: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CONTENT_AVAILABLE] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentContentAvailable(userLastLoginAppDevicePushTargetContentContentAvailable: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CONTENT_AVAILABLE] = userLastLoginAppDevicePushTargetContentContentAvailable
        }
    }

    // User Last Login App Device Push Target Content Mutable Content
    val userLastLoginAppDevicePushTargetContentMutableContent: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_MUTABLE_CONTENT] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentMutableContent(userLastLoginAppDevicePushTargetContentMutableContent: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_MUTABLE_CONTENT] = userLastLoginAppDevicePushTargetContentMutableContent
        }
    }

    // User Last Login App Device Push Target Content Category
    val userLastLoginAppDevicePushTargetContentCategory: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CATEGORY] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentCategory(userLastLoginAppDevicePushTargetContentCategory: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_CATEGORY] = userLastLoginAppDevicePushTargetContentCategory
        }
    }

    // User Last Login App Device Push Target Content Thread ID
    val userLastLoginAppDevicePushTargetContentThreadId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_THREAD_ID] ?: ""
    }

    suspend fun setUserLastLoginAppDevicePushTargetContentThreadId(userLastLoginAppDevicePushTargetContentThreadId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LAST_LOGIN_APP_DEVICE_PUSH_TARGET_CONTENT_THREAD_ID] = userLastLoginAppDevicePushTargetContentThreadId
        }
    }

    // Clear all data
    suspend fun clearAll() {
        Log.d(TAG, "DataStore - Clearing all data")
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d(TAG, "DataStore - All data cleared successfully")
    }

    // Debug method to check all DataStore data
    suspend fun debugDataStoreData() {
        Log.d(TAG, "=== DEBUG DATASTORE DATA ===")
        try {
            val userId = userId.first()
            val userName = userName.first()
            val userEmail = userEmail.first()
            val userPhone = userPhone.first()
            val userToken = userToken.first()
            val userRole = userRole.first()
            val userStatus = userStatus.first()
            val userCreatedAt = userCreatedAt.first()
            val userUpdatedAt = userUpdatedAt.first()
            val userLastLogin = userLastLogin.first()

            Log.d(TAG, "DataStore Debug - User ID: $userId")
            Log.d(TAG, "DataStore Debug - User Name: $userName")
            Log.d(TAG, "DataStore Debug - User Email: $userEmail")
            Log.d(TAG, "DataStore Debug - User Phone: $userPhone")
            Log.d(TAG, "DataStore Debug - User Token: ${userToken.take(20)}...")
            Log.d(TAG, "DataStore Debug - User Role: $userRole")
            Log.d(TAG, "DataStore Debug - User Status: $userStatus")
            Log.d(TAG, "DataStore Debug - User Created At: $userCreatedAt")
            Log.d(TAG, "DataStore Debug - User Updated At: $userUpdatedAt")
            Log.d(TAG, "DataStore Debug - User Last Login: $userLastLogin")

            val hasUserData = userId.isNotEmpty() && userToken.isNotEmpty()
            Log.d(TAG, "DataStore Debug - Has user data: $hasUserData")
            Log.d(TAG, "=== END DEBUG DATASTORE DATA ===")
        } catch (e: Exception) {
            Log.e(TAG, "DataStore Debug - Error reading DataStore data", e)
        }
    }

    suspend fun hasMinimalUserData(): Boolean {
        val userId = userId.firstOrNull().orEmpty()
        val userEmail = userEmail.firstOrNull().orEmpty()
        return userId.isNotBlank() && userEmail.isNotBlank()
    }
} 