/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.security.GsrsSecurityUtils;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.chem.StructureProcessorTask;
import ix.core.models.Structure;
import ix.core.models.Value;
import ix.core.utils.executor.ProcessListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mitch Miller
 */
public class StructureRecalcTaskInitializer extends ScheduledTaskInitializer
{
	@Autowired
	private StructureRepository structureRepository;
	@Autowired
	private RecalcStructurePropertiesService recalcStructurePropertiesService;
	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Autowired
	private AdminService adminService;

	@Override
	@Transactional(readOnly = true)
	public void run(SchedulerPlugin.TaskListener l)
	{
		l.message("Initializing rehashing");
		ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
		{
			if (total != null)
			{
				l.message("Rehashed:" + sofar + " of " + total);
			} else
			{
				l.message("Rehashed:" + sofar);
			}
		});

			List<UUID> ids = structureRepository.getAllIds();
			listen.newProcess();
			listen.totalRecordsToProcess(ids.size());
			//we want to run this in a background thread
			new Thread(()-> {
				ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
				for (UUID id : ids) {

					executor.submit(() -> {
						adminService.runAsAdmin(() -> {
							TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
							try {
								tx.executeWithoutResult(status -> {
									structureRepository.findById(id).ifPresent(s -> {
										listen.preRecordProcess(s);
										try {
											System.out.println("recalcing "+  id);
											recalcStructurePropertiesService.recalcStructureProperties(s);
											System.out.println("done recalcing "+ id);
											listen.recordProcessed(s);
										} catch(Throwable t) {
											listen.error(t);
										}
									});
								});
							} catch (Throwable ex) {
								Logger.getLogger(StructureRecalcTaskInitializer.class.getName()).log(Level.SEVERE, null, ex);
							}
						});
					});
				}

				executor.shutdown();
				try {
					executor.awaitTermination(1, TimeUnit.DAYS );
				} catch (InterruptedException e) {
					//should never happen

				}
				System.out.println("done!!!!");
				listen.doneProcess();
			}).start();

	}

	@Override
	public String getDescription()
	{
		return "Regenerate structure properties collection for all chemicals in the database";
	}

}
