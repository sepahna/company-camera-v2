package ir.mbarani.companycamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.*

private val Navy = Color(0xFF142B4A)
private val Blue = Color(0xFF2563EB)
private val SurfaceSoft = Color(0xFFF4F7FB)

class MainActivity : ComponentActivity() {
 private val vm: CompanyCameraViewModel by viewModels()
 private var capture: ImageCapture? = null
 private var provider: ProcessCameraProvider? = null
 private var lens = CameraSelector.LENS_FACING_BACK
 private lateinit var preview: PreviewView
 private var pendingDescription = ""
 private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) bindCamera() else toast("برای عکاسی مجوز دوربین لازم است.") }
 private val exportJson = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) lifecycleScope.launch { writeJson(uri) } }
 private val exportZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if(uri!=null) lifecycleScope.launch { writeZip(uri) } }
 private val importJson = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) lifecycleScope.launch { restoreJson(uri) } }
 private val importZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) lifecycleScope.launch { restoreZip(uri) } }
 override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); rootDirs(); setContent { CompanyCameraApp(vm, onPreviewReady={ preview=it; bindCamera() }, onCapture={takePhoto(it)}, onFlip={lens=if(lens==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK; bindCamera()}, onBackupJson={exportJson.launch("company-camera-backup.json")}, onBackupZip={exportZip.launch("company-camera-backup.zip")}, onRestoreJson={importJson.launch(arrayOf("application/json","text/*"))}, onRestoreZip={importZip.launch(arrayOf("application/zip","application/octet-stream"))}, onHelp={showHelp()}) }; if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) bindCamera() else cameraPermission.launch(Manifest.permission.CAMERA) }
 private fun rootDir()=File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)?:filesDir,"CompanyCamera").apply{mkdirs()}
 private fun photosRoot()=File(rootDir(),"PHOTOS").apply{mkdirs()}
 private fun backupsRoot()=File(rootDir(),"BACKUPS").apply{mkdirs()}
 private fun categoryDir(c:String)=File(photosRoot(),c).apply{mkdirs()}
 private fun rootDirs(){categoryDir("MACHINERY");categoryDir("PERSONNEL");backupsRoot()}
 private fun bindCamera(){ if(!::preview.isInitialized)return; val future=ProcessCameraProvider.getInstance(this); future.addListener({ try { provider=future.get(); val p=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}; capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(); provider?.unbindAll(); provider?.bindToLifecycle(this,CameraSelector.Builder().requireLensFacing(lens).build(),p,capture) }catch(e:Exception){toast("خطا در دوربین: ${e.localizedMessage}")} },ContextCompat.getMainExecutor(this)) }
 private fun takePhoto(description:String){ val cap=capture?:run{toast("دوربین آماده نیست.");return}; val temp=File(cacheDir,"capture-${System.currentTimeMillis()}.jpg"); cap.takePicture(ImageCapture.OutputFileOptions.Builder(temp).build(),ContextCompat.getMainExecutor(this),object:ImageCapture.OnImageSavedCallback{ override fun onImageSaved(r:ImageCapture.OutputFileResults){ askName(temp,description) }; override fun onError(e:ImageCaptureException){toast("ثبت عکس ناموفق بود: ${e.message}")} }) }
 private fun askName(temp:File,description:String){ val edit=android.widget.EditText(this).apply{hint="نام فایل، مثلاً 2254"}; androidx.appcompat.app.AlertDialog.Builder(this).setTitle("نام عکس").setView(edit).setPositiveButton("ذخیره"){_,_-> val n=edit.text.toString().trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"),"-").removeSuffix(".jpg").removeSuffix(".JPG"); if(n.isBlank()){temp.delete();toast("نام فایل الزامی است.");return@setPositiveButton}; lifecycleScope.launch { val c=vm.category.value; val f=File(categoryDir(c),"$n.jpg"); if(f.exists()){toast("این نام قبلاً ثبت شده است.");temp.delete();return@launch}; withContext(Dispatchers.IO){temp.copyTo(f)}; vm.add(Photo(fileName=f.name,project=c,description=description,relativePath=f.absolutePath,createdAt=System.currentTimeMillis()));temp.delete();toast("عکس ذخیره شد.") } }.setNegativeButton("لغو"){_,_->temp.delete()}.show() }
 private fun showHelp(){val msg="عکس‌ها در مسیر زیر ذخیره می‌شوند:\n${photosRoot().absolutePath}\n\nماشین‌آلات: ${categoryDir("MACHINERY").absolutePath}\nنفرات: ${categoryDir("PERSONNEL").absolutePath}\n\nپشتیبان داخلی: ${backupsRoot().absolutePath}\n\nبرای انتقال به گوشی دیگر، خروجی ZIP بگیرید."; androidx.appcompat.app.AlertDialog.Builder(this).setTitle("راهنمای Company Camera").setMessage(msg).setPositiveButton("بستن",null).show()}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
 private suspend fun backupObject():JSONObject=withContext(Dispatchers.IO){val a=JSONArray();vm.photos.value.forEach{p->val f=File(p.relativePath);val b=if(f.exists())android.util.Base64.encodeToString(f.readBytes(),android.util.Base64.NO_WRAP) else "";a.put(JSONObject().put("fileName",p.fileName).put("project",p.project).put("description",p.description).put("createdAt",p.createdAt).put("imageBase64",b))};JSONObject().put("format","company-camera-backup").put("version",2).put("exportedAt",System.currentTimeMillis()).put("photos",a)}
 private suspend fun writeJson(uri:Uri){try{val bytes=backupObject().toString(2).toByteArray();withContext(Dispatchers.IO){File(backupsRoot(),"backup-${System.currentTimeMillis()}.json").writeBytes(bytes);contentResolver.openOutputStream(uri)?.use{it.write(bytes)}};toast("پشتیبان JSON ذخیره شد.")}catch(e:Exception){toast("خطا: ${e.localizedMessage}")}}
 private suspend fun writeZip(uri:Uri){try{val obj=backupObject();withContext(Dispatchers.IO){val f=File(backupsRoot(),"backup-${System.currentTimeMillis()}.zip");ZipOutputStream(BufferedOutputStream(FileOutputStream(f))).use{z->z.putNextEntry(ZipEntry("backup.json"));z.write(obj.toString(2).toByteArray());z.closeEntry();vm.photos.value.forEach{p->val image=File(p.relativePath);if(image.exists()){z.putNextEntry(ZipEntry("photos/${p.project}/${p.fileName}"));image.inputStream().use{it.copyTo(z)};z.closeEntry()}}};contentResolver.openOutputStream(uri)?.use{out->f.inputStream().use{it.copyTo(out)}}};toast("پشتیبان ZIP ذخیره شد.")}catch(e:Exception){toast("خطا: ${e.localizedMessage}")}}
 private suspend fun restoreJson(uri:Uri){try{val s=withContext(Dispatchers.IO){contentResolver.openInputStream(uri)!!.bufferedReader().readText()};restoreObject(JSONObject(s));toast("بازیابی انجام شد.")}catch(e:Exception){toast("فایل JSON معتبر نیست: ${e.localizedMessage}")}}
 private suspend fun restoreZip(uri:Uri){try{val entries=withContext(Dispatchers.IO){val m=HashMap<String,ByteArray>();ZipInputStream(contentResolver.openInputStream(uri)!!).use{z->while(true){val e=z.nextEntry?:break;if(!e.isDirectory)m[e.name]=z.readBytes();z.closeEntry()}};m};val o=JSONObject(entries["backup.json"]?.toString(Charsets.UTF_8)?:error("backup.json پیدا نشد"));val a=o.getJSONArray("photos");for(i in 0 until a.length()){val item=a.getJSONObject(i);val project=item.optString("project","MACHINERY");val name=item.getString("fileName");val bytes=entries["photos/$project/$name"]?:item.optString("imageBase64").takeIf{it.isNotBlank()}?.let{android.util.Base64.decode(it,android.util.Base64.DEFAULT)};if(bytes!=null)saveRestored(item,bytes)};toast("بازیابی ZIP انجام شد.")}catch(e:Exception){toast("بازیابی ناموفق: ${e.localizedMessage}")}}
 private suspend fun restoreObject(o:JSONObject){require(o.optString("format")=="company-camera-backup");val a=o.getJSONArray("photos");for(i in 0 until a.length()){val p=a.getJSONObject(i);val b=p.optString("imageBase64");if(b.isNotBlank())saveRestored(p,android.util.Base64.decode(b,android.util.Base64.DEFAULT))}}
 private suspend fun saveRestored(p:JSONObject,b:ByteArray)=withContext(Dispatchers.IO){val c=if(p.optString("project")=="PERSONNEL")"PERSONNEL" else "MACHINERY";val n=p.optString("fileName","restored-${System.currentTimeMillis()}.jpg").replace(Regex("[\\\\/:*?\"<>|]+"),"-");var f=File(categoryDir(c),n);if(f.exists())f=File(categoryDir(c),"${System.currentTimeMillis()}-$n");f.writeBytes(b);val db=androidx.room.Room.databaseBuilder(applicationContext,AppDatabase::class.java,"company-camera.db").build();db.photoDao().insert(Photo(fileName=f.name,project=c,description=p.optString("description"),relativePath=f.absolutePath,createdAt=p.optLong("createdAt",System.currentTimeMillis())));db.close()}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyCameraApp(
    vm: CompanyCameraViewModel,
    onPreviewReady: (PreviewView) -> Unit,
    onCapture: (String) -> Unit,
    onFlip: () -> Unit,
    onBackupJson: () -> Unit,
    onBackupZip: () -> Unit,
    onRestoreJson: () -> Unit,
    onRestoreZip: () -> Unit,
    onHelp: () -> Unit
) {
    val photos by vm.photos.collectAsState()
    val category by vm.category.collectAsState()
    val query by vm.query.collectAsState()
    var description by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            secondary = Navy,
            background = SurfaceSoft,
            surface = Color.White
        )
    ) {
        Scaffold(
            containerColor = SurfaceSoft,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "COMPANY CAMERA",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "سامانه ثبت و آرشیو تصاویر کاری",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)
            ) {
                item(key = "stats") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "کل تصاویر",
                            value = photos.size.toString(),
                            icon = Icons.Default.PhotoLibrary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "ماشین‌آلات",
                            value = photos.count { it.project == "MACHINERY" }.toString(),
                            icon = Icons.Default.Build,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "نفرات",
                            value = photos.count { it.project == "PERSONNEL" }.toString(),
                            icon = Icons.Default.Groups,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "camera") {
                    CameraCaptureCard(
                        category = category,
                        description = description,
                        onCategoryChange = vm::setCategory,
                        onDescriptionChange = { description = it },
                        onPreviewReady = onPreviewReady,
                        onFlip = onFlip,
                        onCapture = {
                            onCapture(description)
                            description = ""
                        }
                    )
                }

                item(key = "archive-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "آرشیو تصاویر",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${photos.size} فایل",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = vm::setQuery,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("جست‌وجوی نام فایل؛ مثلاً 2254") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "جست‌وجو")
                            },
                            singleLine = true
                        )
                    }
                }

                val filteredPhotos = photos.filter { photo ->
                    query.isBlank() || photo.fileName.contains(query, ignoreCase = true)
                }

                if (filteredPhotos.isEmpty()) {
                    item(key = "empty-archive") {
                        EmptyArchiveCard()
                    }
                } else {
                    items(
                        items = filteredPhotos,
                        key = { photo -> photo.id }
                    ) { photo ->
                        PhotoArchiveRow(
                            photo = photo,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }

                item(key = "backup") {
                    BackupCard(
                        onBackupJson = onBackupJson,
                        onBackupZip = onBackupZip,
                        onRestoreJson = onRestoreJson,
                        onRestoreZip = onRestoreZip,
                        onHelp = onHelp
                    )
                }
            }
        }

        selectedPhoto?.let { photo ->
            PhotoPreviewDialog(
                photo = photo,
                onDismiss = { selectedPhoto = null }
            )
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("تنظیمات") },
                text = {
                    Text(
                        "Company Camera · نسخه سازمانی\n" +
                            "طراحی Material 3 · ذخیره‌سازی محلی و آفلاین"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) {
                        Text("بستن")
                    }
                }
            )
        }
    }
}

@Composable
private fun CameraCaptureCard(
    category: String,
    description: String,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPreviewReady: (PreviewView) -> Unit,
    onFlip: () -> Unit,
    onCapture: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ثبت تصویر جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Blue)
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    "MACHINERY" to "ماشین‌آلات",
                    "PERSONNEL" to "نفرات"
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = category == option.first,
                        onClick = { onCategoryChange(option.first) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) {
                        Text(option.second)
                    }
                }
            }

            AndroidView(
                factory = { context ->
                    PreviewView(context).also { view ->
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                        onPreviewReady(view)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Color(0xFFE8EEF6), RoundedCornerShape(16.dp))
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("توضیحات عکس (اختیاری)") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onFlip,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تعویض دوربین")
                }
                Button(
                    onClick = onCapture,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("ثبت عکس")
                }
            }
        }
    }
}

@Composable
private fun EmptyArchiveCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(38.dp)
            )
            Text("تصویری پیدا نشد", fontWeight = FontWeight.Medium)
            Text(
                "با ثبت عکس یا تغییر عبارت جست‌وجو دوباره امتحان کنید.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PhotoArchiveRow(
    photo: Photo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEAF1FF),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "مشاهده عکس",
                        tint = Blue,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(photo.fileName, fontWeight = FontWeight.SemiBold)
                Text(
                    if (photo.project == "PERSONNEL") "نفرات" else "ماشین‌آلات",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun BackupCard(
    onBackupJson: () -> Unit,
    onBackupZip: () -> Unit,
    onRestoreJson: () -> Unit,
    onRestoreZip: () -> Unit,
    onHelp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "پشتیبان‌گیری و بازیابی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBackupJson,
                    modifier = Modifier.weight(1f)
                ) { Text("خروجی JSON") }
                Button(
                    onClick = onBackupZip,
                    modifier = Modifier.weight(1f)
                ) { Text("خروجی ZIP") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRestoreJson,
                    modifier = Modifier.weight(1f)
                ) { Text("بازیابی JSON") }
                OutlinedButton(
                    onClick = onRestoreZip,
                    modifier = Modifier.weight(1f)
                ) { Text("بازیابی ZIP") }
            }
            TextButton(onClick = onHelp) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("راهنما و مسیر ذخیره‌سازی")
            }
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    photo: Photo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(photo.fileName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val imageFile = remember(photo.relativePath) { File(photo.relativePath) }
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = photo.fileName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    )
                } else {
                    Text("فایل تصویر در مسیر ذخیره‌شده پیدا نشد.")
                }
                Text(
                    if (photo.project == "PERSONNEL") "نفرات" else "ماشین‌آلات",
                    color = Color.Gray
                )
                if (photo.description.isNotBlank()) {
                    Text(photo.description)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Blue)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
