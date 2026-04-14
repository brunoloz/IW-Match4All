package internal.competicion;

import com.intuit.karate.junit5.Karate;

class InternalRunner {
    
    @Karate.Test
    Karate testCompeticion() {
        return Karate.run("competicion").relativeTo(getClass());
    }      
}
