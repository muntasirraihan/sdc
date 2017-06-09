/**
 * Created by rcohen on 9/15/2016.
 */
'use strict';
import {SchemaProperty} from "app/models";
import {ValidationUtils, PROPERTY_TYPES} from "app/utils";
import {DataTypesService} from "app/services";

export interface ITypeListScope extends ng.IScope {
    parentFormObj:ng.IFormController;
    schemaProperty:SchemaProperty;
    isSchemaTypeDataType:boolean;
    valueObjRef:any;
    propertyNameValidationPattern:RegExp;
    fieldsPrefixName:string;
    readOnly:boolean;
    listDefaultValue:any;
    listNewItem:any;
    maxLength:number;

    getValidationPattern(type:string):RegExp;
    validateIntRange(value:string):boolean;
    addListItem():void;
    deleteListItem(listItemIndex:number):void
}


export class TypeListDirective implements ng.IDirective {

    constructor(private DataTypesService:DataTypesService,
                private PropertyNameValidationPattern:RegExp,
                private ValidationUtils:ValidationUtils) {
    }

    scope = {
        valueObjRef: '=',//ref to list object in the parent value object
        schemaProperty: '=',//get the schema.property object
        parentFormObj: '=',//ref to parent form (get angular form object)
        fieldsPrefixName: '=',//prefix for form fields names
        readOnly: '=',//is form read only
        defaultValue: '@',//this list default value
        maxLength: '='
    };

    restrict = 'E';
    replace = true;
    template = ():string => {
        return require('./type-list-directive.html');
    };

    link = (scope:ITypeListScope, element:any, $attr:any) => {
        scope.propertyNameValidationPattern = this.PropertyNameValidationPattern;

        //reset valueObjRef when schema type is changed
        scope.$watchCollection('schemaProperty.type', (newData:any):void => {
            scope.isSchemaTypeDataType = this.DataTypesService.isDataTypeForSchemaType(scope.schemaProperty);
            //insert 1 empty item dt by default
            if (scope.isSchemaTypeDataType && (!scope.valueObjRef || !scope.valueObjRef.length)) {
                scope.valueObjRef = scope.valueObjRef || [];
                scope.valueObjRef.push({});
            }
        });

        //when user brows between properties in "edit property form"
        scope.$watchCollection('fieldsPrefixName', (newData:any):void => {
            scope.listNewItem = {value: ''};

            if ($attr.defaultValue) {
                scope.listDefaultValue = JSON.parse($attr.defaultValue);
            }
        });

        scope.getValidationPattern = (type:string):RegExp => {
            return this.ValidationUtils.getValidationPattern(type);
        };

        scope.validateIntRange = (value:string):boolean => {
            return !value || this.ValidationUtils.validateIntRange(value);
        };

        scope.addListItem = ():void => {
            scope.valueObjRef = scope.valueObjRef || [];
            let newVal = ((scope.schemaProperty.simpleType || scope.schemaProperty.type) == PROPERTY_TYPES.STRING ? scope.listNewItem.value : JSON.parse(scope.listNewItem.value));
            scope.valueObjRef.push(newVal);
            scope.listNewItem.value = "";
        };

        scope.deleteListItem = (listItemIndex:number):void => {
            scope.valueObjRef.splice(listItemIndex, 1);
            if (!scope.valueObjRef.length) {
                if (scope.listDefaultValue) {
                    angular.copy(scope.listDefaultValue, scope.valueObjRef);
                }
            }
        };
    };

    public static factory = (DataTypesService:DataTypesService,
                             PropertyNameValidationPattern:RegExp,
                             ValidationUtils:ValidationUtils)=> {
        return new TypeListDirective(DataTypesService, PropertyNameValidationPattern, ValidationUtils);
    };
}

TypeListDirective.factory.$inject = ['Sdc.Services.DataTypesService', 'PropertyNameValidationPattern', 'ValidationUtils'];
