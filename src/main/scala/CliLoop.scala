import java.util.{Map => JMap}
import org.jline.keymap.KeyMap
import org.jline.reader.impl.{DefaultParser, LineReaderImpl}
import org.jline.reader.{Binding, Candidate, EndOfFileException, LineReader, LineReaderBuilder, Macro, ParsedLine, Reference, UserInterruptException}
import org.jline.terminal.{Terminal, TerminalBuilder}
import org.jline.utils.AttributedString
import org.jline.utils.InfoCmp.Capability
import collection.JavaConverters._

object CliLoop extends ComplexStringCompleter
  with CustomCompleter
  with CommandCompleter
  with SampleArgumentCompleter
  with SampleRegexCompleter {
  protected val useColor = true

  protected val terminal: Terminal =
    TerminalBuilder.builder
      .system(true)
      .build

  protected val parser: DefaultParser = new DefaultParser
  parser.setEofOnUnclosedQuote(true)

  protected val reader: LineReader = LineReaderBuilder.builder
    .terminal(terminal)
    .completer(treeCompleter)
    .parser(parser)
    .build

  def run(): Unit = {
    help()
    loop()
  }

  def loop(): Unit = {
    val trigger: Option[String] = Some("password")
    val mask: Character = null  // LineReader.readLine uses null to control behavior // TODO set this value
    var more = true

    while (more) {
      var line: String =
      try {
        //reader.readLine(prompt, rightPrompt, null.asInstanceOf[MaskingCallback], null) // right prompt is annoying
        reader.readLine(prompt)
      } catch {
        case _: UserInterruptException =>
          println("Press Control-D to exit")
          ""

        case _: EndOfFileException => return
      }
      if (line != null) {
        line = line.trim

        // If the trigger word is input then the next input line is masked
        if (trigger.exists(line.compareTo(_) == 0))
          line = reader.readLine("password> ", mask)

        if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))
          more = false
        else
          processLine(line)
      }
    }
  }

  def help(full: Boolean = false): Unit = {
    println("Commands are: bindkey, cls, custom, help, set, testkey and tput")
    if (full)
      println("""bindkey - shows all key bindings
                |cls     - clears the screen
                |custom  - demonstrates a TreeCompleter
                |help    - displays this message
                |set     - ???
                |testkey - ???
                |tput    - demonstrates a terminal capability
                |""".stripMargin)
  }

  protected def processLine(line: String): Unit = {
    val parsedLine: ParsedLine = reader.getParser.parse(line, 0)
    parsedLine.word match {
      case "bindkey" => bindKey(parsedLine)

      case "cls" =>
        terminal.puts(Capability.clear_screen)
        terminal.flush()

      case "custom" => custom(parsedLine)

      case "help" | "?" =>
        println
        help()

      case "set" => set(parsedLine)

      case "testkey" => testKey()

      case "tput" => tput(parsedLine)

      case "" =>
        println
        help()

      case x =>
        println(s"Error: '$x' is an unknown command.")
        help()
    }
  }

  protected def custom(pl: ParsedLine): Unit =
    terminal.writer.println(
      s"""parsedLine: word = ${ pl.word }, wordIndex = ${ pl.wordIndex }, wordCursor = ${ pl.wordCursor }, cursor = ${ pl.cursor }
         |words = ${ pl.words.asScala.mkString(", ") }
         |line = ${ pl.line }
         |""".stripMargin
    )

  protected def bindKey(parsedLine: ParsedLine): Unit = {
    if (parsedLine.words.size == 1) {
      val sb = new StringBuilder
      val bound: JMap[String, Binding] = reader.getKeys.getBoundKeys
      bound.entrySet.forEach { entry =>
        sb.append("\"")
        entry.getKey.chars.forEachOrdered { c =>
          if (c < 32) {
            sb.append('^')
            sb.append((c + 'A' - 1).asInstanceOf[Char])
          } else
            sb.append(c.asInstanceOf[Char])
          ()
        }
        sb.append("\" ")
        entry.getValue match {
          case value: Macro =>
            sb.append("\"")
            value.getSequence.chars.forEachOrdered { c =>
              if (c < 32) {
                sb.append('^')
                sb.append((c + 'A' - 1).asInstanceOf[Char])
              } else
                sb.append(c.asInstanceOf[Char])
              ()
            }
            sb.append("\"")

          case reference: Reference =>
            sb.append(reference.name.toLowerCase.replace('_', '-'))

          case _ =>
            sb.append(entry.getValue.toString)
        }
        sb.append("\n")
        ()
      }
      terminal.writer.print(sb.toString)
      terminal.flush()
    } else if (parsedLine.words.size == 3) {
      reader.getKeys.bind(
        new Reference(parsedLine.words.get(2)), KeyMap.translate(parsedLine.words.get(1))
      )
    }
  }

  protected def echoInput(line: String): Unit = {
    terminal.writer.println(
      if (useColor) {
        AttributedString.fromAnsi(s"""\u001B[33m======>\u001B[0m"$line"""").toAnsi(terminal)
      } else {
        s"""======>"$line""""
      }
    )
    terminal.flush()
  }

  protected def set(parsedLine: ParsedLine): Unit = {
    parsedLine.words.size match {
      case 1 =>
        terminal.writer.println("\nError: no variable name or value specified")

      case 2 =>
        terminal.writer.println("\nError: no new value specified for " + parsedLine.words.get(0))

      case 3 =>
        reader.setVariable(parsedLine.words.get(0), parsedLine.words.get(1))

      case n =>
        terminal.writer.println("\nError: Only one new value may be specified " +
          s"(you specified ${n - 2} values for ${parsedLine.words.get(0)})")
    }
  }

  protected def testKey(): Unit = {
    terminal.writer.write("Input the key event (Enter to complete): ")
    terminal.writer.flush()
    val sb = new StringBuilder
    var more = true
    while (more) {
      val c: Int = reader.asInstanceOf[LineReaderImpl].readCharacter
      if (c == 10 || c == 13) more = false
      else sb.append(new String(Character.toChars(c)))
    }
    terminal.writer.println(KeyMap.display(sb.toString))
    terminal.writer.flush()
  }

  protected def tput(parsedLine: ParsedLine): Unit = parsedLine.words.size match {
    case 1 =>
      terminal.writer.println("No capability specified (try 'bell')")

    case 2 =>
      Option(Capability.byName(parsedLine.words.get(1))).map { capability =>
        terminal.puts(capability)
        terminal.flush()
        true
      }.getOrElse {
        terminal.writer.println("Unknown capability")
        false
      }
      ()

    case n =>
      terminal.writer.println(s"Only one capability may be specified (you specified ${ n - 1 })")
  }
}

object Main extends App {
  CliLoop.run()
}
