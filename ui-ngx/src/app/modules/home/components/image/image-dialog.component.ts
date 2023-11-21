///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ChangeDetectorRef, Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { ImageResourceInfo, imageResourceType } from '@shared/models/resource.models';
import {
  UploadImageDialogComponent,
  UploadImageDialogData
} from '@home/components/image/upload-image-dialog.component';
import { UrlHolder } from '@shared/pipe/image.pipe';

export interface ImageDialogData {
  readonly: boolean;
  image: ImageResourceInfo;
}

@Component({
  selector: 'tb-image-dialog',
  templateUrl: './image-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ImageDialogComponent}],
  styleUrls: ['./image-dialog.component.scss']
})
export class ImageDialogComponent extends
  DialogComponent<ImageDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  image: ImageResourceInfo;

  readonly: boolean;

  imageFormGroup: UntypedFormGroup;

  submitted = false;

  imageChanged = false;

  imagePreviewData: UrlHolder;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              private dialog: MatDialog,
              @Inject(MAT_DIALOG_DATA) private data: ImageDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ImageDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.image = data.image;
    this.readonly = data.readonly;
    this.imagePreviewData = {
      url: this.image.link
    };
  }

  ngOnInit(): void {
    this.imageFormGroup = this.fb.group({
      title: [this.image.title, [Validators.required]]
    });
    if (this.data.readonly) {
      this.imageFormGroup.disable();
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(this.imageChanged);
  }

  downloadImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageService.downloadImage(imageResourceType(this.image), this.image.resourceKey).subscribe();
  }

  exportImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    // TODO:
  }

  updateImage($event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<UploadImageDialogComponent, UploadImageDialogData,
      ImageResourceInfo>(UploadImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        image: this.image
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.imageChanged = true;
        this.image = result;
        this.imagePreviewData = {
          url: this.image.link
        };
      }
    });
  }

  save(): void {
    this.submitted = true;
    const title: string = this.imageFormGroup.get('title').value;
    const image = {...this.image, ...{title}};
    this.imageService.updateImageInfo(image).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }
}
