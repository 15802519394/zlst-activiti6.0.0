/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cdi.impl.event;

import java.util.HashSet;
import java.util.Set;

import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BusinessRuleTask;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.ErrorEventDefinition;
import org.activiti.bpmn.model.EventGateway;
import org.activiti.bpmn.model.EventSubProcess;
import org.activiti.bpmn.model.ExclusiveGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.InclusiveGateway;
import org.activiti.bpmn.model.ManualTask;
import org.activiti.bpmn.model.ParallelGateway;
import org.activiti.bpmn.model.ReceiveTask;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.SendTask;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.Task;
import org.activiti.bpmn.model.ThrowEvent;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.bpmn.model.Transaction;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cdi.BusinessProcessEventType;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.parse.BpmnParseHandler;

/**
 * {@link BpmnParseHandler} registering the {@link CdiExecutionListener} for distributing execution events using the cdi event infrastructure
 * 
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class CdiEventSupportBpmnParseHandler implements BpmnParseHandler {

  protected static final Set<Class<? extends BaseElement>> supportedTypes = new HashSet<Class<? extends BaseElement>>();

  static {
    supportedTypes.add(StartEvent.class);
    supportedTypes.add(EndEvent.class);
    supportedTypes.add(ExclusiveGateway.class);
    supportedTypes.add(InclusiveGateway.class);
    supportedTypes.add(ParallelGateway.class);
    supportedTypes.add(ScriptTask.class);
    supportedTypes.add(ServiceTask.class);
    supportedTypes.add(BusinessRuleTask.class);
    supportedTypes.add(Task.class);
    supportedTypes.add(ManualTask.class);
    supportedTypes.add(UserTask.class);
    supportedTypes.add(EndEvent.class);
    supportedTypes.add(SubProcess.class);
    supportedTypes.add(EventSubProcess.class);
    supportedTypes.add(CallActivity.class);
    supportedTypes.add(SendTask.class);
    supportedTypes.add(ReceiveTask.class);
    supportedTypes.add(EventGateway.class);
    supportedTypes.add(Transaction.class);
    supportedTypes.add(ThrowEvent.class);

    supportedTypes.add(TimerEventDefinition.class);
    supportedTypes.add(ErrorEventDefinition.class);
    supportedTypes.add(SignalEventDefinition.class);

    supportedTypes.add(SequenceFlow.class);
  }

  public Set<Class<? extends BaseElement>> getHandledTypes() {
    return supportedTypes;
  }

  public void parse(BpmnParse bpmnParse, BaseElement element) {
    
    if (element instanceof SequenceFlow) {
      
      SequenceFlow sequenceFlow = (SequenceFlow) element;
      CdiExecutionListener listener = new CdiExecutionListener(sequenceFlow.getId());
      addActivitiListenerToElement(sequenceFlow, ExecutionListener.EVENTNAME_TAKE, listener);
      
    } else {
      
      if (element instanceof UserTask) {
        
        UserTask userTask = (UserTask) element;
        
        addCreateListener(userTask);
        addAssignListener(userTask);
        addCompleteListener(userTask);
        addDeleteListener(userTask);
      }
      

      if (element instanceof FlowElement) {
        
        FlowElement flowElement = (FlowElement) element;
        
        addStartEventListener(flowElement);
        addEndEventListener(flowElement);
      }
      
    }
  }

  private void addCompleteListener(UserTask userTask) {
    addActivitiListenerToUserTask(userTask, TaskListener.EVENTNAME_COMPLETE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.COMPLETE_TASK));
  }

  private void addAssignListener(UserTask userTask) {
    addActivitiListenerToUserTask(userTask, TaskListener.EVENTNAME_ASSIGNMENT, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.ASSIGN_TASK));
  }

  private void addCreateListener(UserTask userTask) {
    addActivitiListenerToUserTask(userTask, TaskListener.EVENTNAME_CREATE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.CREATE_TASK));
  }

  protected void addDeleteListener(UserTask userTask) {
    addActivitiListenerToUserTask(userTask, TaskListener.EVENTNAME_DELETE, new CdiTaskListener(userTask.getId(), BusinessProcessEventType.DELETE_TASK));
  }
  
  protected void addStartEventListener(FlowElement flowElement) {
    CdiExecutionListener listener = new CdiExecutionListener(flowElement.getId(), BusinessProcessEventType.START_ACTIVITY);
    addActivitiListenerToElement(flowElement, ExecutionListener.EVENTNAME_START, listener);
  }

  protected void addEndEventListener(FlowElement flowElement) {
    CdiExecutionListener listener = new CdiExecutionListener(flowElement.getId(), BusinessProcessEventType.END_ACTIVITY);
    addActivitiListenerToElement(flowElement, ExecutionListener.EVENTNAME_END, listener);
  }

  protected void addActivitiListenerToElement(FlowElement flowElement, String event, Object instance) {
    ActivitiListener listener = new ActivitiListener();
    listener.setEvent(event);
    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
    listener.setInstance(instance);
    flowElement.getExecutionListeners().add(listener);
  }
  
  protected void addActivitiListenerToUserTask(UserTask userTask, String event, Object instance) {
    ActivitiListener listener = new ActivitiListener();
    listener.setEvent(event);
    listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
    listener.setInstance(instance);
    userTask.getTaskListeners().add(listener);
  }
  
}
