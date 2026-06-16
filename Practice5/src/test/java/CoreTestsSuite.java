import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses(BankAccountTest.class)
@IncludeTags(BankAccountTest.CORE_TESTS_NAME)
class CoreTestsSuite {
}
