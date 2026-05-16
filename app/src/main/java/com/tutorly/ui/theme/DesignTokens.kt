package com.tutorly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Единая точка редактирования отступов. */
object TutorlySpacing {
    /** Минимальный вертикальный отступ между микроэлементами (индикаторы, разделители). */
    val xxs = 3.dp
    /** Очень маленький отступ между иконкой и подписью/строкой. */
    val xs = 4.dp
    /** Малый отступ в плотных группах. */
    val sm = 8.dp
    /** Базовый отступ по умолчанию для большинства блоков. */
    val md = 12.dp
    /** Средний внешний/внутренний отступ секций. */
    val lg = 16.dp
    /** Крупный отступ для карточек/диалогов. */
    val xl = 20.dp
}

/** Радиусы скругления UI-элементов. */
object TutorlyRadii {
    /** Скругление интерактивного айтема таббара. */
    val tabItem = 16.dp
    /** Скругление верхнего активного индикатора в таббаре. */
    val tabIndicator = 2.dp
    /** Капсульный chip/badge. */
    val pill = 999.dp
    /** Скругление верхних углов bottom-sheet. */
    val bottomSheetTop = 28.dp
}

/** Высоты/ширины ключевых контейнеров и индикаторов. */
object TutorlySizing {
    /** Высота нижнего бара приложения. */
    val bottomBarHeight = 80.dp
    /** Высота верхнего бара приложения. */
    val topBarHeight = 80.dp
    /** Толщина верхнего индикатора активной вкладки. */
    val tabIndicatorHeight = 3.dp
    /** Базовый размер иконки в навигационных компонентах. */
    val navIcon = 24.dp
    /** Минимальный размер кружка статуса урока. */
    val statusChipMinSize = 24.dp
    /** Максимальная ширина контейнера диалога на больших экранах. */
    val dialogMaxWidth = 560.dp
    /** Максимальная высота контента диалога перед скроллом. */
    val dialogMaxHeight = 600.dp
}

/** Отдельные токены для локальных паддингов компонентов. */
object TutorlyComponentSpacing {
    /** Отступ между верхним индикатором таба и иконкой. */
    val tabIndicatorToIcon = 9.dp
    /** Горизонтальный паддинг текста в бейдже оплаты. */
    val paymentBadgeHorizontal = 10.dp
}

/** Токены для теней/тональных возвышений. */
object TutorlyElevation {
    /** Тень нижней панели навигации. */
    val bottomBarShadow = 8.dp
    /** Тональная высота нижней панели навигации. */
    val bottomBarTonal = 0.dp
    /** Тень верхнего бара. */
    val topBarShadow = 4.dp
    /** Тональная высота верхнего бара. */
    val topBarTonal = 0.dp
    /** Тональная высота бейджа оплаты. */
    val paymentBadgeTonal = 1.dp
    /** Тональная высота контейнера bottom sheet по умолчанию. */
    val bottomSheetTonal = 0.dp
}

/** Явные токены типографики для мелких бейджей/чипов. */
object TutorlyTypeScale {
    /** Размер текста внутри бейджа оплаты. */
    val badgeText = 12.sp
}

/** Дополнительные цветовые токены для компонентов. */
object TutorlyColors {
    /** Фон нижнего бара. */
    val bottomBarContainer = Color.White
    /** Светлый текст на цветном фоне top bar. */
    val topBarOnGradient = Color(0xFFFEFEFE)
    /** Акцентный цвет успешной оплаты/предоплаты. */
    val paymentPaid = Color(0xFF4E998C)
    /** Нейтральный цвет неоплаченного будущего урока. */
    val futureUnpaid = Color(0xFF727272)
}
