package ca.utoronto.utm.mcs;

import javax.inject.Singleton;

import ca.utoronto.utm.mcs.DaggerModule;
import dagger.Component;

@Singleton
@Component(modules = DaggerModule.class)
public interface BlogComponent {
	
	public Blog buildBlog();

}
