/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gsrs.module.substance.tasks;

import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.chem.StructureProcessorTask;
import ix.core.models.Structure;
import ix.core.models.Value;
import ix.core.utils.executor.ProcessListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

	@Override
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

		try
		{
			TransactionStatus[] status = new TransactionStatus[1];
			new ProcessExecutionService(5, 10).buildProcess(Structure.class)
					.streamSupplier(structureRepository::streamAll)
					.consumer((Structure s) ->
					{
						recalcStructurePropertiesService.recalcStructureProperties(s);
					})
					.before(()->{
						status[0] = platformTransactionManager.getTransaction(TransactionDefinition.withDefaults());
					})
					.after(()->platformTransactionManager.commit(status[0]))
					.listener(listen)
					.build()
					.execute();
		} catch (IOException ex)
		{
			Logger.getLogger(StructureRecalcTaskInitializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public String getDescription()
	{
		return "Regenerate structure properties collection for all chemicals in the database";
	}

}
