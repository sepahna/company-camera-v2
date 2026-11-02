# Company Camera — نسخه تجاری Compose

این بسته سورس کامل پروژه است. ساختار فعلی Room و جدول `photos` و شناسه بسته `ir.mbarani.companycamera` حفظ شده تا نصب نسخه جدید روی نسخه قبلی و دسترسی به دیتابیس قبلی امکان‌پذیر بماند.

## ساخت APK با GitHub Actions
1. فایل ZIP را استخراج کنید.
2. وارد پوشه `CompanyCameraAndroid` شوید.
3. تمام محتویات داخل این پوشه (از جمله پوشه مخفی `.github`) را در ریشه مخزن `company-camera-v2` بارگذاری و فایل‌های قبلی را جایگزین کنید.
4. Commit کنید؛ Workflow با تغییر فایل‌های `app/` اجرا می‌شود.
5. پس از سبز شدن Build، از بخش Artifacts فایل `company-camera-debug-apk` را دریافت کنید.

## نکات
- حداقل Android: API 26
- Compile/Target SDK: 35
- Java: 17
- Gradle: 8.9
- رابط کاربری: Jetpack Compose و Material 3
- الگوی داده: ViewModel + Repository + Room + StateFlow
- آرشیو در حالت فهرست، Bitmap تصویرها را بارگذاری نمی‌کند؛ تصویر با لمس ردیف باز می‌شود.
- مسیر ذخیره عکس‌ها همان مسیر خصوصی اپ در `Android/data/ir.mbarani.companycamera/files/Pictures/CompanyCamera/` است.

## وضعیت اعتبارسنجی
این بسته از روی سورس موجود بازسازی و خطای ساختار LazyColumn در MainActivity برطرف شده است. در این محیط Android SDK/Gradle اجرایی برای تأیید نهایی APK در دسترس نیست؛ بنابراین تأیید قطعی فقط پس از اجرای GitHub Actions انجام می‌شود.
