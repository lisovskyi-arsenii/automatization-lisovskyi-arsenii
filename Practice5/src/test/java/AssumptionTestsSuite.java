import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses(BankAccountTest.class)
@IncludeTags(BankAccountTest.ASSUMPTION_TESTS_NAME)
public class AssumptionTestsSuite {
}
