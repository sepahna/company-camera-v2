# Company Camera – Commercial Compose Edition

نسخه بازطراحی‌شده با Kotlin، Jetpack Compose، Material 3 و معماری MVVM.

## امکانات حفظ‌شده
- ثبت عکس با CameraX و انتخاب دسته ماشین‌آلات/نفرات
- ذخیره فایل‌ها در PHOTOS/MACHINERY و PHOTOS/PERSONNEL
- ثبت نام و توضیحات عکس
- آرشیو با نمایش نام فایل و آیکون مشاهده؛ تصویر اصلی فقط هنگام بازکردن خوانده می‌شود
- جست‌وجوی نام فایل
- خروجی و بازیابی JSON و ZIP
- راهنمای مسیرهای ذخیره‌سازی

## معماری
- UI: Jetpack Compose + Material 3
- State: AndroidX ViewModel + StateFlow
- Data: Room DAO + Repository
- Camera: CameraX
- Image viewer: Coil

## ساخت APK
Workflow در `.github/workflows/build-apk.yml` با GitHub Actions قابل اجراست.

> قبل از انتشار سازمانی، پروژه باید در GitHub Actions ساخته و روی دستگاه واقعی آزمایش شود. این بسته هنوز به‌عنوان APK نهایی تست میدانی نشده است.
