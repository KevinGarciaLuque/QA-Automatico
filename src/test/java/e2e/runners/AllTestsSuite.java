package e2e.runners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({"e2e.login"})
public class AllTestsSuite {
    // no necesita código dentro
}

