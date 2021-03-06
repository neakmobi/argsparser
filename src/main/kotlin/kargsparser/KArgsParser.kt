package kargsparser

import kotlin.text.StringBuilder

/**
 *
 * Реализация парсера аргументов командной строки
 *
 * @property programInfo        Информация о программе (обычно: имя программы, версия и т.д.).
 * @property helpPreamble       Преамбула подсказки по ключу --help.
 * @property applyParams        Функция, обрабатывающая параметры командной строки.
 * @property helpConclusion     Заключение подсказки по ключу --help.
 * @property helpUsage          Подсказка по использованию опций. Если пользователь не предоставляет свою подсказку,
 *                              формируется автоматически.
 * @property manualHelpOption   Пользовательская опция помощи (-h, --help). Поскольку данная опция должна обладать наибольшим
 *                              приоритетом, а также во избежание конфликта с другими опциями и ключами, по умолчанию (true)
 *                              опция --help не добавляется к списку обрабатываемых.
 * @property descriptionIndent  Величина отступа имени опции и ее описания в подсказке (-h, --help <ОТСТУП> description).
 *
 */
class KArgsParser(
        private val programInfo         : String = "",
        private var helpUsage           : String = "",
        private val helpPreamble        : String = "",
        private val helpConclusion      : String = "",
        private var descriptionIndent   : Int = 30,
        private val applyParams         : (Array<String>) -> Boolean = { params -> params.isEmpty() }
) {

    /**
     *
     * @property options             Опции в аргументах командной строки
     * @property requiredControl     Количество опций с флагом "Обязательная"
     * @property helpRequested       Флаг, указывающий что была запрошена помощь
     * @property badOption           Опция, на которой произошла ошибка парсинга
     *
     */
    private val options = mutableListOf<AOption>()
    private var requiredControl = 0
    private var helpRequested = false

    var manualHelpOption: Boolean = true
    var badOption: AOption? = null

    /**
     *
     * Добавление опции к списку ожидаемых опций
     *
     * @param option Добавляемая опция.
     *
     */
    fun addOption(option: AOption) {
        if(option.required) requiredControl++
        options.add(option)
    }

    /**
     *
     * Парсинг аргументов командной строки.
     *
     * @param args Массив аргументов команды запуска
     *
     */
    fun parseArgs(args: Array<String>): ParseResult {

        if(args.isEmpty())
            return if(requiredControl == 0 && applyParams(args)) ParseResult.OK else ParseResult.EMPTY_ARGS

        val argsList = args.toMutableList()

        splitChainKeys(argsList)

        if(!manualHelpOption) {
            addOption(Key(
                    "-h",
                    "--help",
                    "Show this message",
                    Int.MIN_VALUE,
                    false
            ) { println(buildHelp()); helpRequested = true; true })
        }

        options.sortBy { it.priority }

        val applyResult = applyOptions(argsList)
        if(applyResult != ParseResult.OK)
            return applyResult

        if(helpRequested) return ParseResult.HELP_REQUESTED

        if(!applyParams(argsList.toTypedArray()))
            return ParseResult.INVALID_PARAMS

        return ParseResult.OK

    }

    /**
     *
     * Формирование подсказки (обычно по ключу --help или -h)
     *
     * @return Сформированная подсказка
     *
     */
    fun buildHelp(): String {

        val helpBuilder = StringBuilder()
        if (programInfo    != "") helpBuilder.append(programInfo    + "\n"  )
        if (helpUsage      != "") helpBuilder.append(helpUsage      + "\n\n")
        if (helpPreamble   != "") helpBuilder.append(helpPreamble   + "\n\n")
        helpBuilder.append(buildHelpOptions() + "\n")
        if (helpConclusion != "") helpBuilder.append(helpConclusion + "\n"  )

        return helpBuilder.toString()
    }

    /**
     *
     * Принятие опций
     *
     * @param argsList Аргументы программы
     *
     * @return Результат[ParseResult] парсинга опций
     *
     */
    private fun applyOptions(argsList: MutableList<String>): ParseResult {

        var parseResult: OptionParseResult
        for(option in options) {
            parseResult = option.apply(argsList)
            if(parseResult != OptionParseResult.OK) {
                badOption = option
                return when(parseResult) {
                    OptionParseResult.NOT_PROCESSED -> ParseResult.MISSING_REQUIRED_OPTIONS
                    OptionParseResult.ERROR -> ParseResult.INVALID_OPTION
                    OptionParseResult.MISSING_PARAMS -> ParseResult.INVALID_OPTION_PARAMS
                    else -> ParseResult.UNKNOWN_ERROR
                }
            }
            if(option.required) requiredControl--
        }

        if(requiredControl != 0)
            return ParseResult.MISSING_REQUIRED_OPTIONS

        return ParseResult.OK

    }

    /**
     *
     * Формирование подсказки для опций
     *
     * @return Подсказка по использованию опций
     *
     */
    private fun buildHelpOptions(): String {
        val retStr = StringBuilder()
        retStr.append("Options:\n")
        for(option in options) {
            option.descriptionIndent = descriptionIndent
            retStr.append(option.help)
            retStr.append("\n")
        }
        return retStr.toString()
    }

    /**
     *
     * Разбитие связки ключей на отдельные ключи (если связки есть)
     *
     */
    private fun splitChainKeys(argsList: MutableList<String>) {
        val chainKeysRegex = "^[-][^-]..*".toRegex()
        val chainKeys = argsList.find { it.matches(chainKeysRegex) } ?: return
        argsList.remove(chainKeys)
        for(idx in 1 until chainKeys.length)
            argsList.add("-${chainKeys[idx]}")
    }

}