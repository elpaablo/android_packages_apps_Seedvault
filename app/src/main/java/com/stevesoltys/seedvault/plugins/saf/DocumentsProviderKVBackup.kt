package com.stevesoltys.seedvault.plugins.saf

import android.content.Context
import android.content.pm.PackageInfo
import androidx.documentfile.provider.DocumentFile
import com.stevesoltys.seedvault.transport.backup.DEFAULT_QUOTA_KEY_VALUE_BACKUP
import com.stevesoltys.seedvault.transport.backup.KVBackupPlugin
import java.io.IOException
import java.io.OutputStream

@Suppress("BlockingMethodInNonBlockingContext")
internal class DocumentsProviderKVBackup(
    private val storage: DocumentsStorage,
    private val context: Context
) : KVBackupPlugin {

    private var packageFile: DocumentFile? = null

    override fun getQuota(): Long = DEFAULT_QUOTA_KEY_VALUE_BACKUP

    @Throws(IOException::class)
    override suspend fun hasDataForPackage(packageInfo: PackageInfo): Boolean {
        val packageFile =
            storage.currentKvBackupDir?.findFileBlocking(context, packageInfo.packageName)
                ?: return false
        return packageFile.listFiles().isNotEmpty()
    }

    @Throws(IOException::class)
    override suspend fun ensureRecordStorageForPackage(packageInfo: PackageInfo) {
        // remember package file for subsequent operations
        packageFile =
            storage.getOrCreateKVBackupDir().createOrGetDirectory(context, packageInfo.packageName)
    }

    @Throws(IOException::class)
    override suspend fun removeDataOfPackage(packageInfo: PackageInfo) {
        // we cannot use the cached this.packageFile here,
        // because this can be called before [ensureRecordStorageForPackage]
        val packageFile = storage.currentKvBackupDir?.findFileBlocking(context, packageInfo.packageName) ?: return
        packageFile.delete()
    }

    @Throws(IOException::class)
    override suspend fun deleteRecord(packageInfo: PackageInfo, key: String) {
        val packageFile = this.packageFile ?: throw AssertionError()
        packageFile.assertRightFile(packageInfo)
        val keyFile = packageFile.findFileBlocking(context, key) ?: return
        keyFile.delete()
    }

    @Throws(IOException::class)
    override suspend fun getOutputStreamForRecord(
        packageInfo: PackageInfo,
        key: String
    ): OutputStream {
        val packageFile = this.packageFile ?: throw AssertionError()
        packageFile.assertRightFile(packageInfo)
        val keyFile = packageFile.createOrGetFile(context, key)
        return storage.getOutputStream(keyFile)
    }

}