package io.appform.statesman.engine.action.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.appform.statesman.engine.action.ActionHelper;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.template.EvaluatedActionTemplate;
import io.appform.statesman.model.action.template.RuleBasedTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import io.dropwizard.jackson.Jackson;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvaluatedActionTest {
    private EvaluatedAction action;
    private ActionHelper actionHelper;

    @Before
    public void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        actionHelper = mock(ActionHelper.class);
        action = new EvaluatedAction(null, mapper, actionHelper);
    }

    @Test
    public void testEvaluatedAction() {
        EvaluatedActionTemplate template = getTemplate();
        Workflow workflow = Workflow.builder().dataObject(DataObject.builder().data(Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "a")).build()).build();
        when(actionHelper.executeActions(anyListOf(String.class), eq(workflow))).thenReturn(Jackson.newObjectMapper()
                .createObjectNode()
                .put("response", "success"));

        JsonNode response = action.execute(template, workflow);
        Assert.assertEquals("success", response.get("response").asText());

        val actionTemplateListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(actionHelper).executeActions(actionTemplateListArgumentCaptor.capture(), any());
        val value = actionTemplateListArgumentCaptor.<List<String>>getValue();
        Assert.assertEquals(1, value.size());
        Assert.assertEquals("ACTION_A", value.get(0));
    }

    @Test
    public void testEvaluatedActionToChooseDefault() {
        EvaluatedActionTemplate template = getTemplate();
        Workflow workflow = Workflow.builder().dataObject(DataObject.builder().data(Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "d")).build()).build();
        when(actionHelper.executeActions(anyListOf(String.class), eq(workflow))).thenReturn(Jackson.newObjectMapper()
                .createObjectNode()
                .put("response", "success"));

        JsonNode response = action.execute(template, workflow);
        Assert.assertEquals("success", response.get("response").asText());

        val actionTemplateListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(actionHelper).executeActions(actionTemplateListArgumentCaptor.capture(), any());
        val value = actionTemplateListArgumentCaptor.<List<String>>getValue();
        Assert.assertEquals(1, value.size());
        Assert.assertEquals("DEFAULT_ACTION", value.get(0));
    }

    @Test
    public void testEvaluatedActionWithMultipleActionTemplates() {
        EvaluatedActionTemplate template = getTemplate();
        Workflow workflow = Workflow.builder().dataObject(DataObject.builder().data(Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "c")).build()).build();
        when(actionHelper.executeActions(anyListOf(String.class), eq(workflow))).thenReturn(Jackson.newObjectMapper()
                .createObjectNode()
                .put("response", "success"));

        JsonNode response = action.execute(template, workflow);
        Assert.assertEquals("success", response.get("response").asText());

        val actionTemplateListArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(actionHelper).executeActions(actionTemplateListArgumentCaptor.capture(), any());
        val value = actionTemplateListArgumentCaptor.<List<String>>getValue();
        Assert.assertEquals(2, value.size());
        Assert.assertTrue(value.contains("ACTION_C"));
        Assert.assertTrue(value.contains("ACTION_D"));
    }

    @Test()
    public void testEvaluatedActionWhenRuleDoesNotMatch() {
        EvaluatedActionTemplate template = getTemplateWithoutDefaultAction();
        Workflow workflow = Workflow.builder().dataObject(DataObject.builder().data(Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "d")).build()).build();

        JsonNode jsonNode = action.execute(template, workflow);
        Assert.assertEquals(0, jsonNode.size());
    }

    private EvaluatedActionTemplate getTemplate() {
        return EvaluatedActionTemplate.builder()
                .name("test1")
                .ruleBasedTemplates(Lists.newArrayList(
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("ACTION_A"))
                                .rule(" \"$.data.name\" == \"a\" ")
                                .type(RuleBasedTemplate.Type.EVALUATED)
                                .build(),
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("ACTION_B"))
                                .rule(" \"$.data.name\" == \"b\" ")
                                .type(RuleBasedTemplate.Type.EVALUATED)
                                .build(),
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("ACTION_C", "ACTION_D"))
                                .rule(" \"$.data.name\" == \"c\" ")
                                .type(RuleBasedTemplate.Type.EVALUATED)
                                .build(),
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("DEFAULT_ACTION"))
                                .type(RuleBasedTemplate.Type.DEFAULT)
                                .build()
                ))
                .build();
    }

    private EvaluatedActionTemplate getTemplateWithoutDefaultAction() {
        return EvaluatedActionTemplate.builder()
                .name("test1")
                .ruleBasedTemplates(Lists.newArrayList(
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("ACTION_A"))
                                .rule(" \"$.data.name\" == \"a\" ")
                                .type(RuleBasedTemplate.Type.EVALUATED)
                                .build(),
                        RuleBasedTemplate.builder()
                                .actions(Lists.newArrayList("ACTION_B"))
                                .rule(" \"$.data.name\" == \"b\" ")
                                .type(RuleBasedTemplate.Type.EVALUATED)
                                .build()
                ))
                .build();
    }
}