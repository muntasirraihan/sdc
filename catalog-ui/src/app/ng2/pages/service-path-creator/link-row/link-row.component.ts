import {Component, Input} from '@angular/core';
import {DropdownValue} from "app/ng2/components/ui/form-components/dropdown/ui-element-dropdown.component";
import {Link} from './link.model';
import {ServicePathMapItem} from "app/models/graph/nodes-and-links-map";

@Component({
    selector: 'link-row',
    templateUrl: './link-row.component.html',
    styleUrls: ['./link-row.component.less']
})


export class LinkRowComponent {
    @Input() data:Array<ServicePathMapItem>;
    @Input() link:Link;
    @Input() removeRow:Function;
    source:Array<DropdownValue> = [];
    target: Array<DropdownValue> = [];
    srcCP: Array<DropdownValue> = [];
    targetCP: Array<DropdownValue> = [];

    ngOnChanges() {
        if (this.data) {
            this.parseInitialData(this.data);
        }
    }

    parseInitialData(data: Array<ServicePathMapItem>) {
        this.source = this.convertValuesToDropDownOptions(data);
        if (this.link.fromNode) {
            let srcCPOptions = this.findOptions(data, this.link.fromNode);
            if (!srcCPOptions) { return; }
            this.srcCP = this.convertValuesToDropDownOptions(srcCPOptions);
            if (this.link.fromCP) {
                let targetOptions = this.findOptions(srcCPOptions, this.link.fromCP);
                if (!targetOptions) { return; }
                this.target = this.convertValuesToDropDownOptions(targetOptions);
                if (this.link.toNode) {
                    let targetCPOptions = this.findOptions(targetOptions, this.link.toNode);
                    if (!targetCPOptions) { return; }
                    this.targetCP = this.convertValuesToDropDownOptions(targetCPOptions);
                }
            }
        }
    }

    private findOptions(items: Array<ServicePathMapItem>, nodeOrCPId: string) {
        let item = items.find((dataItem)=> nodeOrCPId === dataItem.id);
        if (item && item.data && item.data.options) {
            return item.data.options;
        }
        console.warn('no option was found to match selection of Node/CP with id:' + nodeOrCPId);
        return null;
    }

    private convertValuesToDropDownOptions(values: Array<ServicePathMapItem>) : Array<DropdownValue> {
        let result = [];
        for (let i = 0; i < values.length ; i++) {
            result[result.length] =  new DropdownValue(values[i].id, values[i].data.name);
        }
        return result;
    }

    onSourceSelected(id) {
        if (id) {
            let srcCPOptions = this.findOptions(this.data, id);
            this.srcCP = this.convertValuesToDropDownOptions(srcCPOptions);
            this.link.fromCP = '';
            this.link.toNode = '';
            this.link.toCP = '';
            this.target = [];
            this.targetCP = [];
        }
    }

    onSrcCPSelected (id) {
        if (id) {
            let srcCPData = this.data.find((dataItem)=> this.link.fromNode === dataItem.id).data;
            let srcCPOptions = srcCPData.options;
            let targetOptions = this.findOptions(srcCPOptions, id);
            this.target = this.convertValuesToDropDownOptions(targetOptions);
            this.link.fromCPOriginId = srcCPData.ownerId;
            this.link.toNode = '';
            this.link.toCP = '';
            this.targetCP = [];
        }

    }

    onTargetSelected(id) {
        if (id) {
            let srcCPOptions = this.findOptions(this.data, this.link.fromNode);
            let targetOptions = this.findOptions(srcCPOptions, this.link.fromCP);
            let targetCPOptions = this.findOptions(targetOptions, id);
            this.targetCP = this.convertValuesToDropDownOptions(targetCPOptions);
            this.link.toCP = '';
        }

    }

    onTargetCPSelected(id) {
        if (id) {
            let srcCPOptions = this.findOptions(this.data, this.link.fromNode);
            let targetOptions = this.findOptions(srcCPOptions, this.link.fromCP);
            let targetCPOptions = this.findOptions(targetOptions, this.link.toNode);
            let targetCPDataObj = targetCPOptions.find((dataItem)=> id === dataItem.id).data;
            this.link.toCPOriginId = targetCPDataObj.ownerId;
        }
    }
}