package test

import org.specs2.mutable._

class UtilitySpec extends Specification {
  "htmlEscape" should {
    "identify" in {
      controllers.Utility.htmlEscape("abcdefg") mustEqual "abcdefg"
    }
    "convert '&' to &amp;" in {
      controllers.Utility.htmlEscape("abc&efg") mustEqual "abc&amp;efg"
    }
    "convert '<' to &lt;" in {
      controllers.Utility.htmlEscape("abc<efg") mustEqual "abc&lt;efg"
    }
    "convert '>' to &gt;" in {
      controllers.Utility.htmlEscape("abc>efg") mustEqual "abc&gt;efg"
    }
    "convert '\"' to &amp;" in {
      controllers.Utility.htmlEscape("abc\"efg") mustEqual "abc&quot;efg"
    }
    "convert ''' to &amp;" in {
      controllers.Utility.htmlEscape("abc'efg") mustEqual "abc&#039;efg"
    }
  }
}
