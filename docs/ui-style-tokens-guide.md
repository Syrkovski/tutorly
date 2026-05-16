# UI Style Tokens Guide

Этот документ описывает, как менять стили элементов через токены в `app/src/main/java/com/tutorly/ui/theme/DesignTokens.kt`.

## Общие правила
- Не меняйте размеры/позиционирование напрямую в компонентах.
- Для отступов используйте `TutorlySpacing`.
- Для радиусов используйте `TutorlyRadii`.
- Для размеров (высоты/ширины/минимальные размеры) используйте `TutorlySizing`.
- Для локальных интервалов конкретных компонентов используйте `TutorlyComponentSpacing`.
- Для теней/тональных возвышений используйте `TutorlyElevation`.
- Для мелкой типографики используйте `TutorlyTypeScale`.
- Для служебных цветов компонентов используйте `TutorlyColors`.

## Как менять стили по элементам

### AppBottomBar
Файл: `ui/components/AppBottomBar.kt`
- Высота бара: `TutorlySizing.bottomBarHeight`.
- Горизонтальный внутренний отступ: `TutorlySpacing.md`.
- Скругление кнопки вкладки: `TutorlyRadii.tabItem`.
- Толщина/скругление верхнего индикатора активной вкладки: `TutorlySizing.tabIndicatorHeight`, `TutorlyRadii.tabIndicator`.
- Расстояние от индикатора до иконки: `TutorlyComponentSpacing.tabIndicatorToIcon`.
- Размер иконки вкладки: `TutorlySizing.navIcon`.
- Цвет фона бара: `TutorlyColors.bottomBarContainer`.
- Тень/тональность: `TutorlyElevation.bottomBarShadow`, `TutorlyElevation.bottomBarTonal`.

### TopBar
Файл: `ui/components/TopBar.kt`
- Высота бара: `TutorlySizing.topBarHeight`.
- Цвет текста и иконок на градиенте: `TutorlyColors.topBarOnGradient`.
- Тень/тональность контейнера: `TutorlyElevation.topBarShadow`, `TutorlyElevation.topBarTonal`.

### PaymentBadge
Файл: `ui/components/PaymentBadge.kt`
- Радиус капсулы: `TutorlyRadii.pill`.
- Размер текста: `TutorlyTypeScale.badgeText`.
- Горизонтальный паддинг: `TutorlyComponentSpacing.paymentBadgeHorizontal`.
- Вертикальный паддинг: `TutorlySpacing.xs`.
- Цвет успешной оплаты/предоплаты: `TutorlyColors.paymentPaid`.
- Тональность: `TutorlyElevation.paymentBadgeTonal`.

### StatusChip
Файл: `ui/components/StatusChip.kt`
- Минимальный размер чипа: `TutorlySizing.statusChipMinSize`.
- Цвет paid: `TutorlyColors.paymentPaid`.
- Цвет future unpaid: `TutorlyColors.futureUnpaid`.

### TutorlyBottomSheetContainer
Файл: `ui/components/TutorlyBottomSheetContainer.kt`
- Скругление верхних углов: `TutorlyRadii.bottomSheetTop`.
- Отступы области drag handle: `TutorlySpacing.md`, `TutorlySpacing.xs`.
- Тональная высота по умолчанию: `TutorlyElevation.bottomSheetTonal`.

### TutorlyDialog
Файл: `ui/components/TutorlyDialog.kt`
- Горизонтальный внешний отступ диалога: `TutorlySpacing.lg`.
- Внутренние отступы контента: `TutorlySpacing.xl`, `TutorlySpacing.lg`.
- Расстояние между блоками контента: `TutorlySpacing.xl`.
- Максимальная ширина/высота: `TutorlySizing.dialogMaxWidth`, `TutorlySizing.dialogMaxHeight`.
