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

/** Высоты ключевых контейнеров и индикаторов. */
object TutorlySizing {
    /** Высота нижнего бара приложения. */
    val bottomBarHeight = 80.dp
    /** Высота верхнего бара приложения. */
    val topBarHeight = 80.dp
    /** Толщина верхнего индикатора активной вкладки. */
    val tabIndicatorHeight = 3.dp
    /** Базовый размер иконки в навигационных компонентах. */
    val navIcon = 24.dp
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
