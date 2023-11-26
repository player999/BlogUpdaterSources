package org.zakharchenko.taras.blogutils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.zakharchenko.taras.blogutils.BlogConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlogSsh {
    private TransportConfigCallback transport_configuration_callback;
    private BlogConfiguration configuration;
    public BlogSsh(BlogConfiguration config) {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);

                // Load the private key
                defaultJSch.addIdentity(config.getPrivateKeyPath());

                return defaultJSch;
            }
        };
        SshSessionFactory.setInstance(sshSessionFactory);

        transport_configuration_callback = transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        };

        configuration = config;
    }

    private void updateRepo(String directory, String branch) throws IOException, GitAPIException, InterruptedException {
        Git git = null;
        if(Files.exists(Paths.get(directory))) {
            git = Git.open(new File(directory));
            git.fetch().setTransportConfigCallback(transport_configuration_callback).call();
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + branch).call();
            git.close();
        } else {
            Git.cloneRepository().setURI(configuration.getRepoUri())
                    .setBranch(branch)
                    .setDirectory(new File(directory))
                    .setTransportConfigCallback(transport_configuration_callback)
                    .call();
        }
    }

    public void updateSourceRepo() throws GitAPIException, IOException, InterruptedException {
        updateRepo("blog", "master");
    }

    public void updateDestinationRepo() throws GitAPIException, IOException, InterruptedException {
        updateRepo("blog_publish", "gh-pages");
    }

    private void pushRepo(String directory) throws IOException, GitAPIException {
        Git git = Git.open(new File(directory));
        git.add().addFilepattern(".").call();
        CommitCommand cmtcmd = git.commit().setAuthor(configuration.getFullName(), configuration.getEmail()).
                setCommitter(configuration.getFullName(), configuration.getEmail());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime currentDateTime = LocalDateTime.now();
        cmtcmd.setMessage(currentDateTime.format(formatter)).call();

        PushCommand pushcmd = git.push();
        pushcmd = pushcmd.setForce(true);
        pushcmd = pushcmd.setTransportConfigCallback(transport_configuration_callback);
        pushcmd = pushcmd.setRemote(configuration.getRepoUri());
        pushcmd.call();
    }

    public void pushSourceRepo() throws GitAPIException, IOException {
        pushRepo("blog");
    }

    public void pushDestinationRepo() throws GitAPIException, IOException {
        pushRepo("blog_publish");
    }

}
