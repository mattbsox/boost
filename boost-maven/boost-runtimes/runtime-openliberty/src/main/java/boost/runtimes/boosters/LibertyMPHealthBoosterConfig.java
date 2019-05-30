package boost.runtimes.boosters;

import static io.openliberty.boost.common.config.ConfigConstants.MPHEALTH_10;
import java.util.Map;
import io.openliberty.boost.common.boosters.MPHealthBoosterConfig;
import io.openliberty.boost.common.BoostException;
import io.openliberty.boost.common.BoostLoggerI;
import boost.runtimes.LibertyServerConfigGenerator;
import boost.runtimes.boosters.LibertyBoosterI;

public class LibertyMPHealthBoosterConfig extends MPHealthBoosterConfig implements LibertyBoosterI {

    public LibertyMPHealthBoosterConfig(Map<String, String> dependencies, BoostLoggerI logger) throws BoostException {
        super(dependencies, logger);
    }

    @Override
	public String getFeature() {
        if (getVersion().equals(MP_20_VERSION)) {
            return MPHEALTH_10;
        }
        return null;
    }

    @Override
	public void addServerConfig(LibertyServerConfigGenerator libertyServerConfigGenerator) {
        
    }
}